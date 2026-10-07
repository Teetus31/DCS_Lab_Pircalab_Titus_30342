package lab.scd.net.socket;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class CalculatorClient extends JFrame {

	private JTextField firstField = new JTextField(10);
	private JTextField secondField = new JTextField(10);
	private JTextField resultField = new JTextField(10);

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	public CalculatorClient(Socket socket) throws IOException {
		super("Network Calculator");
		this.socket = socket;

		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		// Output is automatically flushed
		// by PrintWriter:
		out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

		resultField.setEditable(false);

		// two input fields + result field
		JPanel fields = new JPanel(new GridLayout(3, 2, 5, 5));
		fields.add(new JLabel("Number 1:"));
		fields.add(firstField);
		fields.add(new JLabel("Number 2:"));
		fields.add(secondField);
		fields.add(new JLabel("Result:"));
		fields.add(resultField);

		// four operation buttons
		JPanel buttons = new JPanel(new GridLayout(1, 4, 5, 5));
		String[] ops = { "+", "-", "*", "/" };
		for (String op : ops) {
			JButton b = new JButton(op);
			b.addActionListener(e -> calculate(op));
			buttons.add(b);
		}

		JPanel root = new JPanel(new BorderLayout(10, 10));
		root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		root.add(fields, BorderLayout.CENTER);
		root.add(buttons, BorderLayout.SOUTH);
		setContentPane(root);

		// when the window closes, send END so the server closes the connection
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				out.println("END");
				try {
					socket.close();
				} catch (IOException ex) {
				}
				System.exit(0);
			}
		});

		pack();
		setLocationRelativeTo(null);
	}

	private void calculate(String op) {
		String a = firstField.getText().trim();
		String b = secondField.getText().trim();

		try {
			Double.parseDouble(a);
			Double.parseDouble(b);
		} catch (NumberFormatException ex) {
			resultField.setText("Enter two numbers");
			return;
		}

		try {
			out.println(a + " " + op + " " + b); // send request
			String reply = in.readLine(); // wait for response

			if (reply == null) {
				resultField.setText("Server disconnected");
			} else if (reply.startsWith("RESULT ")) {
				resultField.setText(reply.substring(7));
			} else if (reply.startsWith("ERROR ")) {
				resultField.setText(reply.substring(6));
			} else {
				resultField.setText(reply);
			}
		} catch (IOException ex) {
			resultField.setText("Connection error");
		}
	}

	public static void main(String[] args) throws Exception {
		// creating object address that identifies the server address
		InetAddress address = InetAddress.getByName("localhost");
		// the alternative could be used: InetAddress.getByName ("127.0.0.1")
		Socket socket = new Socket(address, 1900);

		CalculatorClient client = new CalculatorClient(socket);
		SwingUtilities.invokeLater(() -> client.setVisible(true));
	}
}
