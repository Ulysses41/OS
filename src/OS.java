import java.awt.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.omg.CORBA.DATA_CONVERSION;
import org.omg.CORBA.PUBLIC_MEMBER;


enum Interrupt_Type{
		No_interrupt,
		Power_off_interrupt, //断电中断
		Power_on_interrupt,
		Running_Null_interrupt,//如果指向正在执行进程的变量为空时，发生此中断
		Process_end_interrupt,
		I_or_O_interrupt,
		Time_interrupt,
		Idle_Over_Occupancy_interrupt,
		Other_interrupt
}

enum Process_State{
	Undefined,
	Running,
	Ready,
	Block
}

enum Block_Type{
	No_Block,
	A,
	B
}

class PCB{
	int id;
	
	//idle进程默认占用内存块大小，设置一个标志位来标明本进程是否是idle进程
	public static final int size_of_idle_block = 3;//2块指令，1块数据
	public boolean is_idle;
	
	public static Queue<PCB> ready_queue = new LinkedList<PCB>();
	public static Queue<PCB> block_queue = new LinkedList<PCB>();
	
	
	//相关寄存器的备份
	Interrupt_Type PSW_PCB = Interrupt_Type.No_interrupt;
	char[] IR_PCB = null;
	PC_struct PC_PCB = null;
	
	Block_Info block_Info = null;
	
	//为本进程分配的内存块的所在位置的集合
	SetList<Integer> alloted_memory = null;
	SetList<Integer> PC_PCB_IRs = null;//用来存放要传给pc的内存地址队列
	Process_State state = Process_State.Undefined;
	String file_path = null;
	String output_folder = null;
	
	int file_row = 0;//读入文件需要的内存块
	int total_mem_block = 0;//标明需要申请多少内存块
	
	
	
	
	//创建idle进程
	public PCB() {
		this.id = 0;
		this.is_idle = true;
		alloted_memory = new SetList<Integer>();
		PC_PCB = new PC_struct();
		PC_PCB_IRs = new SetList<Integer>();
		
		block_Info = new Block_Info();
		block_Info.reason = Block_Type.No_Block;
		block_Info.block_time = 0;
		
		Memory.allot(this, PCB.size_of_idle_block);
		
		//将申请到的内存块地址复制到pc要读取的队列中
		Iterator<Integer> it = alloted_memory.iterator();
		while(it.hasNext()) PC_PCB_IRs.add(it.next());
		
		load_to_idle();//将idle代码加载到内存块中
		
		PC_PCB.next_IR = PC_PCB_IRs.pollFirst();//将代码的头地址放入本进程的PC副本中，以供寄存器pc调用
		
		this.state = Process_State.Ready;
		ready_queue.add(this);
	}
	//创建普通进程
	PCB(int id,String file_path,String output_folder){
		this.id = id;
		this.file_path = file_path;
		this.output_folder = output_folder;
		this.is_idle = false;
		
		//计算文件的行数从而得到需要申请的内存块数
		file_row = get_blocks_needed_to_allot(file_path);
		total_mem_block = file_row + 1;//运算结果占用1个内存块
		
		//为新建进程分配内存
		alloted_memory = new SetList<Integer>();
		PC_PCB_IRs = new SetList<Integer>();
		PC_PCB = new PC_struct();
		block_Info = new Block_Info();
		block_Info.reason = Block_Type.No_Block;
		block_Info.block_time = 0;
		
		Memory.allot(this, total_mem_block);
		
		//将申请到的内存块地址复制到pc要读取的队列中
		Iterator<Integer> it = alloted_memory.iterator();
		while(it.hasNext()) PC_PCB_IRs.add(it.next());
		
		load_to_memory(file_path);
		
		PC_PCB.next_IR = PC_PCB_IRs.pollFirst();//将代码的头地址放入本进程的PC副本中，以供寄存器pc调用
		
		//令进程进入就绪队列
		this.state = Process_State.Ready;
		ready_queue.add(this);
	}
	
	//进程的撤销
	public void destory(){
		//如果不是idle进程，将结果写入输出文件
		if(!is_idle)write_to_disk(alloted_memory);
		
		//撤销内存
		Memory.countermand(this);
		//销毁对象需要将指向本对象的指针置空，由上层方法设计
	}
	
	
	//进程阻塞
	public void block(Interrupt_Type PSW_PCB,char[] IR_PCB,PC_struct pc){
		//保存相关寄存器内容
		this.PSW_PCB = PSW_PCB;
		this.IR_PCB = IR_PCB;
		this.PC_PCB.PCB_id = pc.PCB_id;
		
		if(this.state!=Process_State.Block){
		block_queue.add(this);
		this.state = Process_State.Block;
		}
	}
	
	//进程唤醒
	public void awaken(){
		if(this.state!=Process_State.Ready){
			if(this.state==Process_State.Block){
				block_queue.remove(this);
			}
			ready_queue.add(this);
			this.state = Process_State.Ready;
		}
	}
	
	public void load_to_idle(){
		String idle_code[] = {"x=0;","gob;"};
		int last_block = alloted_memory.getLast();
		int certain_block;
		Iterator<Integer> it = alloted_memory.iterator();
		while(it.hasNext()){
			certain_block = it.next();
			if(certain_block==last_block)break;
			for(int j=0;j<Memory.size_of_block;j++){
				Memory.mem[certain_block][j] = idle_code[certain_block].charAt(j);
			}
		}	
	}
	
	
	//将数据放入申请的内存块儿中
	public void load_to_memory(String path){
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(path));
			String s = null;
			int certain_block = 0;
			Iterator<Integer> it = alloted_memory.iterator();
			while((s=br.readLine())!=null){
				if(it.hasNext()){
					certain_block = it.next();
					if(certain_block==alloted_memory.getLast())break;
					for(int j=0;j<Memory.size_of_block;j++){
						Memory.mem[certain_block][j] = s.charAt(j);
					}
				}
			}
			br.close();
		}catch(FileNotFoundException e){
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	//写进程在内存中保存的结果到输出文件
	public void write_to_disk(SetList<Integer> am){
		String output_file = output_folder + "\\00" + id;
		File f = new File(output_file);
		//如果文件存在则删除并新建
		if(f.exists()){
			f.delete();
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			if(am.size()>0){
				int last_block = am.getLast();
				char c[] = new char[Memory.size_of_block];
				for(int i=0;i<Memory.size_of_block;i++){
					c[i] = Memory.mem[last_block][i];
				}
				bw.write(c);	
				String send_to_GUI_running_results = "00"+this.id+"->"+new String(c);//同时还要将结果传递给即将要打包给界面的缓存中
				System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
				System.out.println("send:"+send_to_GUI_running_results);
				send_to_GUI_running_results += "\n";
				Pack_To_GUI.running_results = send_to_GUI_running_results;
				
			}
			bw.close();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void print_mem(){
		if(alloted_memory.size()>0){
			int certain_block;
			System.out.println("alloted memory:");
			Iterator<Integer> it = alloted_memory.iterator();
			while(it.hasNext()){
				certain_block = it.next();
				System.out.print(certain_block+":");
				for(int j=0;j<Memory.size_of_block;j++){
					System.out.print(Memory.mem[certain_block][j]);
				}
				System.out.println();
			}
			System.out.println();
		}
	}
	
	public void print_PC_PCB_IRs(){
		if(PC_PCB_IRs.size()>0){
			System.out.println("PCB_IRs:");
			Iterator<Integer> it = PC_PCB_IRs.iterator();
			while(it.hasNext()){
				System.out.print(it.next()+" ");
			}
			System.out.println();
		}
		
	}
	
	public static void print_ready_queue(){
		Iterator<PCB> it = PCB.ready_queue.iterator();
		System.out.println("Ready_Queue id:");
		while(it.hasNext()){
			System.out.print(it.next().id+" ");
		}
		System.out.println();
	}
	
	public static void print_block_queue(){
		Iterator<PCB> it = PCB.block_queue.iterator();
		System.out.println("Block_Queue id:");
		while(it.hasNext()){
			System.out.print(it.next().id+" ");
		}
		System.out.println();
	}
	
	
	//获取文件行数
	public int get_blocks_needed_to_allot(String path){
		FileReader fr;
		String s;
		
		int line_num = 0;
		try {
			fr = new FileReader(path);
			LineNumberReader lnr = new LineNumberReader(fr);
			
			while(lnr.readLine()!=null) line_num++;
			lnr.close();
			fr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line_num;
		
	}
}

class Memory{
	public static final int size_of_block = 4;
	public static final int block_num = 200;
	public static SetList<Integer> allot = new SetList<Integer>();
	public static SetList<Integer> free = new SetList<Integer>();
	public static char[][] mem = new char[block_num][size_of_block];
	
	
	
	Memory(){
		for(int i=0;i<block_num;i++)
			free.add(i);
	}
	
	public static int get_allot_num(){
		return allot.size();
	}
	
	public static int get_free_num(){
		return free.size();
	}
	
	public static boolean allot(PCB pcb,int blocks) {

		// TODO Auto-generated method stub
		for(int i=0;i<blocks;i++){
			if(free.size()>0){
				pcb.alloted_memory.add(free.getFirst());
				allot.add(free.getFirst());
				free.removeFirst();
				
			}
			else{
				System.err.println("memory is null!");
				return false;
			}
		}
		return true;
	}
	
	//回收进程i的已分配的空间
	public static void countermand(PCB pcb){
		Iterator<Integer> it = pcb.alloted_memory.iterator();
		while(it.hasNext()){
			int block_allot = it.next();
			allot.remove(new Integer(block_allot));
			free.add(new Integer(block_allot));	
			
		}
		pcb.alloted_memory.clear();
		pcb.alloted_memory = null;
	}
}

class Pack_To_GUI{
	String process_id;
	int present_timeslice;
	public static String running_results = "";//用于进程撤销前写入其运行结果
	String present_instruction,intermediate_result,
		ready_queue,block_queue;
	SetList<Integer> memory_alloted;
	Pack_To_GUI(){
		memory_alloted = new SetList();
	}
	public void print(){
		System.out.println("Pack_To_GUI:");
		System.out.println("\tprocess_id:" + process_id);
		System.out.println("\tpresent_timeslice:"+present_timeslice);
		System.out.println("\trunning_results:"+running_results);
		System.out.println("\tpresent_instruction:"+present_instruction);
		System.out.println("\tintermediate_result:"+intermediate_result);
		System.out.println("\tready_queue:"+ready_queue);
		System.out.println("\tblock_queue:"+block_queue);
		System.out.print("\tmemory_alloted:");
		Iterator<Integer> it = memory_alloted.iterator();
		while(it.hasNext()){
			System.out.print(it.next() + " ");
		}
		System.out.println();
	}
	
	public void pack(){
		process_id = "00" + OS.running_process.id;
		if(OS.running_process.is_idle)process_id = "idle";
		
		present_timeslice = OS.timer;		
		present_instruction = new String(OS.IR);	
		intermediate_result = String.valueOf(OS.DR);
		
		ready_queue = "";
		Iterator<PCB> it1 = PCB.ready_queue.iterator();
		while(it1.hasNext()){
			ready_queue += get_file_name_with_process_id(it1.next().id);
			ready_queue += "\n";
		}
		block_queue = "";
		Iterator<PCB> it2 = PCB.block_queue.iterator();
		while(it2.hasNext()){
			block_queue += get_file_name_with_process_id(it2.next().id);
			block_queue += "\n";
		}
		
		Iterator<Integer> it = Memory.allot.iterator();
		while(it.hasNext()){
			memory_alloted.add(it.next());
		}
		System.out.println("Pack_to_GUI:allot:"+memory_alloted.size());
	}
	
	public void pack_mem_allot_clear(){
		memory_alloted.clear();
	}
	
	String get_file_name_with_process_id(int id){
		return id==0?"idle":"00"+id;
	}
}

class PC_struct{
	int PCB_id;
	int next_IR;
}

class Block_Info{
	Block_Type reason = null;
	int block_time;
}

class OS {
	public static boolean is_power_on = false;
	
	public static final String INPUT_FILE_DIRECTORY = "Input_file";
	public static final String OUTPUT_FILE_DIRECTORY = "Output_file";
	public static final int IR_Length = 4;//标明指令长度
	public static final int File_num = 10;//标明载入进程的数量
	public static final int Timeslice = 5;//标明时间片长度
	public static final int single_timeslice_millisecond = 200;
	/*一个时间片的实际时长，用线程睡眠来模拟，单位(ms)
	 * 时长最好大于等于内存的块数（200）
	 * 否则refresh_mem_bar()的动态改变效果可能不明显
	*/
	
	public static boolean is_process_handled = false;
	
	//用来实现中断字的组合
	public static final Queue<Interrupt_Type> PSW_CACHE = new LinkedList<Interrupt_Type>();
	Pack_To_GUI pack_to_GUI = null;//每个时间片需要将当前状态显示到GUI
	Recv_Pack recv_Pack = null;
	
	//相关寄存器
	public static Interrupt_Type PSW = null;
	public static char[] IR = null;
	public static PC_struct pc = null;
	public static int DR;
	public static int timer;
	
	
	
	public static PCB running_process = null;
	PCB[] process = null;
	Memory memory = null;
	
	OS(){
		PSW = Interrupt_Type.No_interrupt;
		IR = new char[4];
		pc = new PC_struct();
		timer = Timeslice;
		
		memory = new Memory();
		process = new PCB[File_num+1];
		process[0] = new PCB();//idle进程
		pack_to_GUI = new Pack_To_GUI();
		recv_Pack = new Recv_Pack();
	}
	//中断的优先级设定
	//默认Running_Null和Idle_Over_Occupancy的中断优先级最高，只要发生中断就处理
	//其他的可以等这个时间片结束后处理
	
	void CPU(){
		while(true){
			if(is_power_on){
				if(PSW_CACHE.size()>0){
					//遍历中断字队列，依序进行中断处理
					Iterator<Interrupt_Type> it = PSW_CACHE.iterator();
					while(it.hasNext()){
						if(running_process==null) interrupt_handling(Interrupt_Type.Running_Null_interrupt);
						interrupt_handling(it.next());
					}
					PSW_CACHE.clear();
					is_process_handled = false;
				}
				if(running_process==null){
					interrupt_handling(Interrupt_Type.Running_Null_interrupt);
					is_process_handled = false;
				}
				
				//防止idle进程过度占用处理机
				if((running_process.is_idle)&&(PCB.ready_queue.size()>=1)){
					interrupt_handling(Interrupt_Type.Idle_Over_Occupancy_interrupt);
					is_process_handled = false;
				}
				pc.PCB_id = running_process.id;
				running_process.state = Process_State.Running;
				System.out.println("timer:"+timer);
				//取running_process中的pc_PCB的next_IR，并传到寄存器pc的next_IR中
				pc.next_IR = running_process.PC_PCB.next_IR;
				//令running_process中的pc的next_IR指向下一条地址
				
				running_process.PC_PCB.next_IR = running_process.PC_PCB_IRs.poll();
				
				
				//寄存器IR取指
				for(int i=0;i<IR_Length;i++) IR[i] = Memory.mem[pc.next_IR][i];
				
				System.out.println("running_process id:"+running_process.id);
				//执行指令解释方法,可能产生的中断,记录在PSW_CACHE中
				instruction_parse(running_process);
				pack_to_GUI.pack();
				recv_Pack.recv(pack_to_GUI);
				pack_to_GUI.pack_mem_allot_clear();
				OS_GUI.refresh_OS_GUI();
				OS_GUI.refresh_bar();
				
				
				
				PCB.print_ready_queue();
				PCB.print_block_queue();
				
				System.out.println("IR:"+String.valueOf(IR)+"\n\n");
				
				//定时器--
				timer--;
				if(timer==0){
					PSW = Interrupt_Type.Time_interrupt;
					PSW_CACHE.add(Interrupt_Type.Time_interrupt);
				}
				block_queue_timer_sub();
				
			}
			try {
				Thread.sleep(single_timeslice_millisecond);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
	//中断处理，同时也是进程调度
	public void interrupt_handling(Interrupt_Type interrupt){
		switch (interrupt) {
		//默认running为空时，idle位于ready队列，即ready不为空
		case Power_off_interrupt:
			
			break;
		case Power_on_interrupt:
			power_on();
			break;
		case Running_Null_interrupt:
			process_handling();
			is_process_handled = true;
			break;
		case Process_end_interrupt:
			running_process.destory();
			process[running_process.id] = null;
			running_process = null;
			break;
		case I_or_O_interrupt:
			running_process.block(PSW, IR, pc);
			running_process = null;
			break;
		case Time_interrupt:
			process_handling();
			is_process_handled = true;
			timer = Timeslice;
			break;
		case Idle_Over_Occupancy_interrupt:
			process_handling();
			is_process_handled = true;
			break;
		case Other_interrupt:System.out.println("Other interrupt!");
			break;
		default:System.err.println("Error in interrupt!");
			break;
		}
	}
	
	public static void power_off(){
		is_power_on = false;
		System.out.println("power off!");
	}
	
	public static void power_on(){
		is_power_on = true;
		System.out.println("power on!");
	}
	
	public static void reset(){
		for(int i=0;i<=File_num;i++){
			
		}
	}
	
	public void process_handling(){
		//如果此次还没有发生过进程调度，则
		if(!is_process_handled){
			if(running_process==null){
				running_process = PCB.ready_queue.poll();
			}
			else{
				PCB.ready_queue.add(running_process);
				running_process = null;
				running_process = PCB.ready_queue.poll();
			}
		}
		
	}
	
	public void block_queue_timer_sub(){
		if(PCB.block_queue.size()>0){
			PCB temp_block = null;
			Iterator<PCB> it = PCB.block_queue.iterator();
			while(it.hasNext()){
				temp_block = it.next();
				//如果阻塞时间自减后发现为0，就唤醒并加到就绪队列
				if(temp_block.block_Info.block_time>0)
					temp_block.block_Info.block_time--;
			}
			temp_block = PCB.block_queue.peek();
			if(temp_block.block_Info.block_time==0){
				temp_block.awaken();
			}
		}	
	}
	//IR指令解析
	public void instruction_parse(PCB running_process){
		String IR_s = new String(IR);
		int last_block;//获取该进程的数据区位置
		last_block = running_process.alloted_memory.getLast();
		
		char data[] = null;//存放该进程数据区的内容
		String data_s = null;//数据区内容的字符串表示
		int x;//字符串转为int型
		if(IR_s.charAt(0)=='!'){//如果指令是中断类型
			if(IR_s.charAt(1)=='A'){
				char c = IR_s.charAt(2);
				int i = c - '0';
				running_process.block_Info.reason = Block_Type.A;
				running_process.block_Info.block_time = i;
			}
			else if(IR_s.charAt(1)=='B'){
				char c = IR_s.charAt(2);
				int i = c - '0';
				running_process.block_Info.reason = Block_Type.B;
				running_process.block_Info.block_time = i;
			}
			else{
				System.out.println("Other interrupt!");
			}
			//默认中断不能是0秒
			PSW = Interrupt_Type.I_or_O_interrupt;
			PSW_CACHE.add(Interrupt_Type.I_or_O_interrupt);
		}
		else{
			switch (IR_s) {//该进程的数据区放0
			case "x=0;":Memory.mem[last_block][0] = '0';
					DR = 0;
				break;
			case "x=1;":Memory.mem[last_block][0] = '1';
					DR = 1;
				break;
			case "x++;"://将该进程位于数据区的内容取出来并++，再放回数据区
					data = new char[4];
					for(int i=0;i<Memory.size_of_block;i++) data[i] = Memory.mem[last_block][i];
					data_s = new String(data);
					for(int j=1;j<Memory.size_of_block;j++){
						if(Integer.valueOf(data_s.charAt(j))==0){
							data_s = data_s.substring(0, j);
							break;
						}
					}
					
					x = Integer.parseInt(data_s);
					x++;
					DR = x;
					data_s = String.valueOf(x);
					//for循环的次数取决于内存块长度(4)与数字长度之间较小者
					for(int i=0;i<(Memory.size_of_block<data_s.length()?Memory.size_of_block:data_s.length());i++){
						Memory.mem[last_block][i] = data_s.charAt(i);
					}
				
				break;
			case "x--;"://同x++的过程
				data = new char[4];
				for(int i=0;i<Memory.size_of_block;i++) data[i] = Memory.mem[last_block][i];
				data_s = new String(data);
				for(int j=1;j<Memory.size_of_block;j++){
					if(Integer.valueOf(data_s.charAt(j))==0){
						data_s = data_s.substring(0, j);
						break;
					}
				}
				x = Integer.parseInt(data_s);
				x--;
				DR = x;
				data_s = String.valueOf(x);
				//for循环的次数需取内存块长度(4)与数字长度之间较小者
				for(int i=0;i<(Memory.size_of_block<data_s.length()?Memory.size_of_block:data_s.length());i++){
					Memory.mem[last_block][i] = data_s.charAt(i);
				}
				
				break;
			case "end;":PSW = Interrupt_Type.Process_end_interrupt;
				PSW_CACHE.add(Interrupt_Type.Process_end_interrupt);
				break;
			case "gob;"://清空运行进程中的pc的next_IR队列，重新获取，并将队头指向头指令所在的地址
					//且放于pc在该进程的pc备份中
					running_process.PC_PCB_IRs.clear();
					Iterator<Integer> it = running_process.alloted_memory.iterator();
					while(it.hasNext()) running_process.PC_PCB_IRs.offer(it.next());
					running_process.PC_PCB.next_IR = running_process.PC_PCB_IRs.poll();
					
				break;
			default:System.out.println("Undefined instruction!");
				break;
			}
		}
		
		
	}
	
	
	
	
	
	
	

}

class SetList<T> extends LinkedList<T> {
	private static final long serialVersionUID = 1434324234L;

	@Override
	public boolean add(T object) {
		if (size() == 0) {
			return super.add(object);
		} else {
			int count = 0;
			for (T t : this) {
				if (t.equals(object)) {
					count++;
					break;
				}
			}
			if (count == 0) {
				return super.add(object);
			} else {
				return false;
			}
		}
	}
}
