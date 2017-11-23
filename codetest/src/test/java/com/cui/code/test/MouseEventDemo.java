package com.cui.code.test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MouseEventDemo {

	private Frame frame;
	private Button button;
	private TextField tf;

	public MouseEventDemo() {
		init();
	}

	private void init() {

		frame = new JFrame("�¼�����");
		frame.setLocation(300, 200);
		frame.setSize(300, 400);
		frame.setLayout(new FlowLayout());
		frame.setVisible(true);

		button = new Button("���ҡ���");
		tf= new TextField(30);
		
		frame.add(button);
		frame.add(tf);

		myEvent();

	}

	private void myEvent() {
		// button.addActionListener(new ActionListener() {
		//
		// public void actionPerformed(ActionEvent e) {
		// System.out.println(e.getActionCommand());
		// }
		// });

		tf.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if (!(code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9)) {
					System.out.println("���������֡���");
					e.consume();
				}

			}
		});

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				switch (e.getClickCount()) {
				case 1:
					System.out.println("�����һ�£�");
					break;
				case 2:
					System.out.println("��������£�");
					break;
				case 3:
					System.out.println("��������£�");
					break;
				case 4:
					System.out.println("��������£�");
					break;
				default:
					System.out.println("����˰ɣ�����ô������껵�����˰�");
				}
			}
		});

		button.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				System.out.println(KeyEvent.getKeyText(e.getKeyCode()) + "..."
						+ e.getKeyCode());
			}
		});

	}

	public static void main(String[] args) {
		new MouseEventDemo();
	}

}
