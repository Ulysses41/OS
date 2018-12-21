import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.BorderLayout;

import javax.swing.JLabel;

import java.awt.GridLayout;
import java.awt.Canvas;
import java.util.Iterator;

import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

public class OS_GUI extends JFrame implements ActionListener{
	public static int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
	public static int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
	//获取屏幕分辨率
	public int window_width = 800;
	public int window_height = 600;
	//设置窗口的大小
	
	
	private JPanel bottom_jp = null,memory_bottom_jp = null,
			process_info_jp = null,present_instruction_jp = null,
			process_running_jp = null,timeslice_jp = null,
			intermediate_result_jp = null,ready_queue_jp = null,
			block_queue_jp = null,running_results_jp = null,
			console_jp = null;
	
	private static Memory_Bar memory_bar_display_jp = null;
			
	private JButton power_jb = null,loading_into_process_jb = null;
	
	private JLabel running_process_jl = null,timeslice_jl = null,
			present_instruction_jl = null,intermediate_result_jl = null,
			ready_queue_jl = null,block_queue_jl = null,running_results_jl = null;
	
	
	private static JTextField running_process_jtf = null;
	private static JTextField timeslice_jtf = null;
	private static JTextField present_instruction_jtf = null;
	private static JTextField intermediate_result_jtf = null;
			
	private static JTextArea ready_queue_jta = null;
	private static JTextArea block_queue_jta = null;
	private static JTextArea running_results_jta = null;
	
	private static MemoryJPanel[] mem_jp;
	
	//为实现动态变化，设置两个变量保存内存改变前的块数和改变后的块数
	public static int old_blocks,new_blocks;
	//保存改变前后的差值，设置差值计数器，每次“++”或“--”，直到两者相等
	public static int spanning_length,spanning_length_count;
	
	public OS os = null;
	
	
	public OS_GUI() {
		super("OS");
		bottom_jp = new JPanel();
		this.setContentPane(bottom_jp);
		bottom_jp.setBackground(new Color(230,240,245));
		//设置窗口居中
		this.setBounds((screen_width-window_width)/2, (screen_height-window_height)/2, window_width, window_height);

		bottom_jp.setLayout(null);

		memory_bottom_jp = new JPanel();
		memory_bottom_jp.setBackground(new Color(230,240,245));
		memory_bottom_jp.setBounds(231, 140, 400, 400);
		bottom_jp.add(memory_bottom_jp);
		memory_bottom_jp.setLayout(new GridLayout(10, 20, 1, 2));
		
		mem_jp = new MemoryJPanel[Memory.block_num];
		for(int i=0;i<Memory.block_num;i++){
			MemoryJPanel jpp_ = new MemoryJPanel(i,19,38);
			mem_jp[i] = jpp_;
			memory_bottom_jp.add(mem_jp[i]);
			jpp_ = null;
		}
		
		process_info_jp = new JPanel();
		process_info_jp.setBackground(new Color(230,240,245));
		process_info_jp.setBounds(14, 13, 500, 114);
		bottom_jp.add(process_info_jp);
		process_info_jp.setLayout(null);
		
		process_running_jp = new JPanel();
		process_running_jp.setBounds(20, 0, 100, 114);
		process_info_jp.add(process_running_jp);
		process_running_jp.setLayout(new BorderLayout(0, 0));
		
		running_process_jl = new JLabel("\u8FD0\u884C\u7684\u8FDB\u7A0B");
		running_process_jl.setBackground(new Color(255, 255, 255));
		running_process_jl.setHorizontalAlignment(SwingConstants.CENTER);
		process_running_jp.add(running_process_jl, BorderLayout.NORTH);
		
		running_process_jtf = new JTextField();
		running_process_jtf.setEditable(false);
		running_process_jtf.setBackground(new Color(144, 238, 144));
		running_process_jtf.setHorizontalAlignment(SwingConstants.CENTER);
		process_running_jp.add(running_process_jtf, BorderLayout.CENTER);
		running_process_jtf.setColumns(10);
		
		timeslice_jp = new JPanel();
		timeslice_jp.setBounds(140, 0, 100, 114);
		process_info_jp.add(timeslice_jp);
		timeslice_jp.setLayout(new BorderLayout(0, 0));
		
		
		timeslice_jl = new JLabel("\u65F6\u95F4\u7247");
		timeslice_jl.setBackground(new Color(255, 255, 255));
		timeslice_jl.setHorizontalAlignment(SwingConstants.CENTER);
		timeslice_jp.add(timeslice_jl, BorderLayout.NORTH);
		
		timeslice_jtf = new JTextField();
		timeslice_jtf.setEditable(false);
		timeslice_jtf.setHorizontalAlignment(SwingConstants.CENTER);
		timeslice_jtf.setBackground(new Color(175, 238, 238));
		timeslice_jp.add(timeslice_jtf, BorderLayout.CENTER);
		timeslice_jtf.setColumns(10);
		
		present_instruction_jp = new JPanel();
		present_instruction_jp.setBounds(260, 0, 100, 114);
		process_info_jp.add(present_instruction_jp);
		present_instruction_jp.setLayout(new BorderLayout(0, 0));
		
		present_instruction_jl = new JLabel("\u5F53\u524D\u6307\u4EE4");
		present_instruction_jl.setHorizontalAlignment(SwingConstants.CENTER);
		present_instruction_jp.add(present_instruction_jl, BorderLayout.NORTH);
		
		present_instruction_jtf = new JTextField();
		present_instruction_jtf.setEditable(false);
		present_instruction_jtf.setHorizontalAlignment(SwingConstants.CENTER);
		present_instruction_jtf.setBackground(new Color(255, 182, 193));
		present_instruction_jp.add(present_instruction_jtf, BorderLayout.CENTER);
		present_instruction_jtf.setColumns(10);
		
		intermediate_result_jp = new JPanel();
		intermediate_result_jp.setBounds(380, 0, 100, 114);
		process_info_jp.add(intermediate_result_jp);
		intermediate_result_jp.setLayout(new BorderLayout(0, 0));
		
		intermediate_result_jl = new JLabel("\u4E2D\u95F4\u7ED3\u679C");
		intermediate_result_jl.setHorizontalAlignment(SwingConstants.CENTER);
		intermediate_result_jp.add(intermediate_result_jl, BorderLayout.NORTH);
		
		intermediate_result_jtf = new JTextField();
		intermediate_result_jtf.setEditable(false);
		intermediate_result_jtf.setHorizontalAlignment(SwingConstants.CENTER);
		intermediate_result_jtf.setBackground(new Color(255, 255, 224));
		intermediate_result_jp.add(intermediate_result_jtf, BorderLayout.CENTER);
		intermediate_result_jtf.setColumns(10);
		
		console_jp = new JPanel();
		console_jp.setBackground(new Color(230,240,245));
		console_jp.setBounds(633, 13, 135, 114);
		bottom_jp.add(console_jp);
		console_jp.setLayout(null);
		
		Icon power_icon = new ImageIcon("Icon/power.jfif");
		power_jb = new JButton("\u7535\u6E90");
		power_jb.setBorderPainted(false);
		power_jb.setFocusPainted(false);
		power_jb.setIcon(power_icon);
		power_jb.setBounds(30, 0, 77, 83);
		power_jb.addActionListener(this);
		console_jp.add(power_jb);
		
		loading_into_process_jb = new JButton("\u8F7D\u5165\u8FDB\u7A0B");
		loading_into_process_jb.setBounds(14, 87, 113, 27);
		loading_into_process_jb.addActionListener(this);
		console_jp.add(loading_into_process_jb);
		
		ready_queue_jp = new JPanel();
		ready_queue_jp.setBounds(14, 140, 90, 400);
		bottom_jp.add(ready_queue_jp);
		ready_queue_jp.setLayout(new BorderLayout(0, 0));
		
		ready_queue_jl = new JLabel("\u5C31\u7EEA\u961F\u5217");
		ready_queue_jl.setHorizontalAlignment(SwingConstants.CENTER);
		ready_queue_jp.add(ready_queue_jl, BorderLayout.NORTH);
		
		ready_queue_jta = new JTextArea();
		ready_queue_jta.setLineWrap(true);
		ready_queue_jta.setEditable(false);
		ready_queue_jp.add(ready_queue_jta, BorderLayout.CENTER);
		
		block_queue_jp = new JPanel();
		block_queue_jp.setBounds(118, 140, 90, 400);
		bottom_jp.add(block_queue_jp);
		block_queue_jp.setLayout(new BorderLayout(0, 0));
		
		block_queue_jl = new JLabel("\u963B\u585E\u961F\u5217");
		block_queue_jl.setHorizontalAlignment(SwingConstants.CENTER);
		block_queue_jp.add(block_queue_jl, BorderLayout.NORTH);
		
		block_queue_jta = new JTextArea();
		block_queue_jta.setLineWrap(true);
		block_queue_jta.setEditable(false);
		block_queue_jp.add(block_queue_jta, BorderLayout.CENTER);
		
		running_results_jp = new JPanel();
		running_results_jp.setBounds(655, 140, 90, 400);
		bottom_jp.add(running_results_jp);
		running_results_jp.setLayout(new BorderLayout(0, 0));
		
		running_results_jl = new JLabel("\u7A0B\u5E8F\u8FD0\u884C\u7ED3\u679C");
		running_results_jl.setHorizontalAlignment(SwingConstants.CENTER);
		running_results_jp.add(running_results_jl, BorderLayout.NORTH);
		
		running_results_jta = new JTextArea();
		running_results_jta.setLineWrap(true);
		running_results_jta.setEditable(false);
		running_results_jp.add(running_results_jta, BorderLayout.CENTER);
		
		memory_bar_display_jp = new Memory_Bar();
		memory_bar_display_jp.setBorder(new LineBorder(new Color(127, 255, 212)));
		memory_bar_display_jp.setBounds(14, 541, 731, 10);
		bottom_jp.add(memory_bar_display_jp);
		
		old_blocks = new_blocks = 0;
		spanning_length = spanning_length_count = 0;
		
		
		
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getActionCommand().equals("电源")){
			if(!OS.is_power_on)OS.power_on();
			else OS.power_off();
		}
		if(e.getActionCommand().equals("载入进程")){
			//创建10个普通的进程
			if(os.is_power_on){
				for(int i=1;i<=os.File_num;i++){
					String s = "Input_file\\00" + i + ".txt";
					os.process[i] = new PCB(i,s, os.OUTPUT_FILE_DIRECTORY);
				}
			}
			else{
				new JOptionPane().showMessageDialog(null,"程序暂停无法载入", "提示",JOptionPane.ERROR_MESSAGE);
			}
			
		}
		
	}
	
	public static void refresh_OS_GUI(){
		running_process_jtf.setText(Recv_Pack.process_id);
		timeslice_jtf.setText(String.valueOf(Recv_Pack.present_timeslice));
		present_instruction_jtf.setText(Recv_Pack.present_instruction);
		intermediate_result_jtf.setText(Recv_Pack.intermediate_result);
		
		
		ready_queue_jta.setText(Recv_Pack.ready_queue);
		block_queue_jta.setText(Recv_Pack.block_queue);
		
		/*因为不知道程序什么时候将运行结果写入打包方法中，故将之设置成静态成员
		 * 界面每次刷新时都会判断其是否为空，
		 * 不为空则追加到相应文本区域的尾部
		 * */ 
		if((Pack_To_GUI.running_results!=null)&&("".equals(Pack_To_GUI.running_results)==false)){
			running_results_jta.append(Pack_To_GUI.running_results);
			Pack_To_GUI.running_results = null;
		}
		int block_alloted = Recv_Pack.memory_alloted.size();
		for(int i=0;i<Memory.block_num;i++){
			mem_jp[i].is_alloted = false;
			mem_jp[i].repaint();
			Iterator<Integer> it = Recv_Pack.memory_alloted.iterator();
			while(it.hasNext()){
				int certain_block = it.next();
				if(certain_block==i){
					mem_jp[i].is_alloted = true;
					mem_jp[i].repaint();
					break;
				}
			}
		}
		int loop_count;
		try{
			loop_count = Math.abs(OS_GUI.spanning_length);
			if(loop_count==0)refresh_bar();
			else{
				for(int i=1;i<=loop_count;i++){
					Thread.sleep(OS.single_timeslice_millisecond/loop_count);
					refresh_bar();
				}
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void refresh_bar(){
		if(spanning_length==spanning_length_count){
			old_blocks = new_blocks;
			new_blocks = Recv_Pack.memory_alloted.size();

			spanning_length = new_blocks - old_blocks;
			if(spanning_length<0) spanning_length_count = -1;
			else if(spanning_length==0) spanning_length_count = 0;
			else spanning_length_count = 1;
		}
		memory_bar_display_jp.repaint();
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			JFrame.setDefaultLookAndFeelDecorated(true);
			UIManager.setLookAndFeel("com.jtattoo.plaf.fast.FastLookAndFeel");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OS_GUI os_gui = new OS_GUI();
		os_gui.os = new OS();
		
		os_gui.os.CPU();
		
	}

}

class Memory_Bar extends JPanel{

	int my_pix;
	double old_mem_percent,new_mem_percent;
	
	
	int block_ratio_pix;//指一个内存块应该在内存条图中占据的像素
	int rect_;//进度条的宽度
	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		super.paint(g);
		my_pix = this.getWidth();
		block_ratio_pix = my_pix/Memory.block_num;
		old_mem_percent = (double)OS_GUI.old_blocks / Memory.block_num;
		new_mem_percent = (double)OS_GUI.new_blocks / Memory.block_num;
		rect_ = (int)(old_mem_percent*my_pix);
		
		g.setColor(new Color(135,206,235));
		g.fillRect(0, 0, rect_ + block_ratio_pix * OS_GUI.spanning_length_count,this.getHeight());
		if(OS_GUI.spanning_length_count!=OS_GUI.spanning_length){
			if(OS_GUI.spanning_length<0)OS_GUI.spanning_length_count--;
			else if(OS_GUI.spanning_length==0) OS_GUI.spanning_length_count = 0;
			else OS_GUI.spanning_length_count++;
		}

	}
	
}


class Recv_Pack{
	public static String process_id;
	public static int present_timeslice;
	public static String running_results = "";//用于进程撤销前写入其运行结果
	public static String present_instruction,intermediate_result,
		ready_queue,block_queue;
	public static SetList<Integer> memory_alloted = new SetList();
	
	public void print(){
		System.out.println("Recv_Pack:");
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
	
	public void recv(Pack_To_GUI pt){
		process_id = pt.process_id;
		present_timeslice = pt.present_timeslice;
		running_results = pt.running_results;
		present_instruction = pt.present_instruction;
		intermediate_result = pt.intermediate_result;
		ready_queue = pt.ready_queue;
		block_queue = pt.block_queue;
		
		if(memory_alloted.size()>0)memory_alloted.clear();//重新录入前应清除上回的记录
		Iterator<Integer> it = pt.memory_alloted.iterator();
		while(it.hasNext()){
			memory_alloted.add(it.next());
		}
	}
	
}



class MemoryJPanel extends JPanel{
	public int mem_id;
	boolean is_alloted = false;
	public MemoryJPanel(int id,int width, int height) {
		this.mem_id = id;
		this.setSize(width, height);
	}
	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		super.paint(g);
		if(!is_alloted) this.setBackground(Color.GRAY);
		else this.setBackground(Color.ORANGE);
		
	}
}