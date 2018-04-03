import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

public class WebServerTest {
	// 资源根路径
	public static final String WEB_ROOT = System.getProperty("user.dir");

	// 关闭命令
	public static final String SHUTDOWN_COMMAND = "/SHUTDOWN";
	// 判断是否是关闭服务器请求
	public static boolean shutdown = false;

	public static void main(String[] args) {
		int count = 0;
		while (!shutdown) {
			try {
				// 创建一个服务器端socket，即serversocket，监听8888端口
				ServerSocket serverSocket = new ServerSocket(8888);
				Socket socket = null;
				while (!shutdown) {
					socket = serverSocket.accept();// 循环等待，获取客户端连接
					System.out.println("访问数量:" + (++count));
					InetAddress address = socket.getInetAddress();
					System.out.println("客户端ip地址:" + address.getHostAddress());
					new HttpServerThread(socket).start();
					Thread.sleep(1000);//让出cpu 否则执行一次shutdown关不掉
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

class HttpServerThread extends Thread {
	Socket socket = null;

	public HttpServerThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = socket.getInputStream();
			output = socket.getOutputStream();

			// 接收请求并解析
			Request request = new Request(input);
			request.parse();

			// 响应
			Response response = new Response(output);
			response.setRequest(request);
			response.sendStaticResource();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 关闭资源
			try {
				if (input != null)
					input.close();
				if (output != null)
					output.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

class Request {

	private InputStream input;
	private String uri="";

	public Request(InputStream input) {
		this.input = input;
	}

	public void parse() {
		StringBuffer request = new StringBuffer(2048);
		int i;
		byte[] buffer = new byte[2048];
		try {
			i = input.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			i = -1;
		}
		for (int j = 0; j < i; j++) {
			request.append((char) buffer[j]);
		}
		System.out.print(request.toString());
		uri = parseUri(request.toString());
	}

	private String parseUri(String requestString) {
		int index1, index2;
		index1 = requestString.indexOf(' ');
		if (index1 != -1) {
			index2 = requestString.indexOf(' ', index1 + 1);
			if (index2 > index1)
				return requestString.substring(index1 + 1, index2);
		}
		return null;
	}

	public String getUri() {
		System.out.println("请求资源:" + uri);
		return uri;
	}

}

class Response {

	private static final int BUFFER_SIZE = 1024;
	Request request = null;
	OutputStream output = null;

	public Response(OutputStream output) {
		this.output = output;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public void sendStaticResource() throws IOException {
		// 判断是否关闭服务器
		WebServerTest.shutdown = request.getUri().contains(WebServerTest.SHUTDOWN_COMMAND);
		if (WebServerTest.shutdown) {
			String errorMessage = "<!DOCTYPE html>\r\n" + "<html>\r\n" + "<head>\r\n" + "<meta charset=\"UTF-8\">\r\n"
					+ "<title>关闭服务器</title>\r\n" + "</head>\r\n" + "<body>\r\n" + "服务器已关闭\r\n" + "</body>\r\n"
					+ "</html>";
			output.write(errorMessage.getBytes());
		} else {
			byte[] bytes = new byte[BUFFER_SIZE];
			FileInputStream fis = null;
			try {
				File file = new File(WebServerTest.WEB_ROOT, request.getUri());
				if (file.exists()) {
					fis = new FileInputStream(file);
					int ch = fis.read(bytes, 0, BUFFER_SIZE);
					while (ch != -1) {
						output.write(bytes, 0, ch);
						ch = fis.read(bytes, 0, BUFFER_SIZE);
					}
				} else {
					System.out.println("未找到请求资源");
					// 请求资源未找到
					String errorMessage = "<!DOCTYPE html>\r\n" + "<html>\r\n" + "<head>\r\n"
							+ "<meta charset=\"UTF-8\">\r\n" + "<title>404</title>\r\n" + "</head>\r\n" + "<body>\r\n"
							+ "请求资源未找到\r\n" + "</body>\r\n" + "</html>";
					output.write(errorMessage.getBytes());
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			} finally {
				if (fis != null)
					fis.close();
			}
		}
	}
}
