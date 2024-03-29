/*
 * IncomingMessageFrame.java
 * 
 * Created on Mar 25, 2004
 *
 */
package gov.nist.applet.phone.ua.gui;

import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 * 
 *         <a href="{@docRoot} /uncopyright.html">This code is in the public
 *         domain.</a>
 */
public class IncomingMessageFrame extends JFrame {
	ChatFrame chatFrame;
	String caller;

	/** Creates new form ConfigurationFrame */
	public IncomingMessageFrame(ChatFrame chatFrame, String caller) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI(this);
		} catch (Exception exe) {
			exe.printStackTrace();
		}
		this.chatFrame = chatFrame;
		this.caller = caller;
		initComponents();
		this.setSize(410, 130);
		this.setResizable(false);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents() {// GEN-BEGIN:initComponents
		jPanel1 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jButton1 = new javax.swing.JButton();
		jButton2 = new javax.swing.JButton();

		getContentPane().setLayout(null);

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Incoming Call");
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				exitForm(evt);
			}
		});

		jPanel1.setLayout(null);

		jLabel1.setText(caller
				+ " is trying to contact you, do you want to answer ?");
		jPanel1.add(jLabel1);
		jLabel1.setBounds(20, 15, 390, 20);

		jButton1.setText("Yes");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jYesButtonActionPerformed(evt);
			}
		});

		jPanel1.add(jButton1);
		jButton1.setBounds(75, 45, 55, 32);

		jButton2.setText("No");
		jButton2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jNoButtonActionPerformed(evt);
			}
		});

		jPanel1.add(jButton2);
		jButton2.setBounds(225, 45, 55, 32);

		getContentPane().add(jPanel1);
		jPanel1.setBounds(0, 0, 410, 130);

		pack();
	}// GEN-END:initComponents

	private void jYesButtonActionPerformed(java.awt.event.ActionEvent evt) {
		chatFrame.answerOK("sip:" + caller);
		dispose();
	}

	private void jNoButtonActionPerformed(java.awt.event.ActionEvent evt) {
		chatFrame.answerBusy("sip:" + caller);
		dispose();
	}

	private void exitForm(WindowEvent evt) {
		chatFrame.answerBusy("sip:" + caller);
		dispose();
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton jButton1;
	private javax.swing.JButton jButton2;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JPanel jPanel1;
	// End of variables declaration//GEN-END:variables

}
