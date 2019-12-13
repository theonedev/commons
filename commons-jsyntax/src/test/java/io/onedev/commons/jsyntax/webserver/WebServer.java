package io.onedev.commons.jsyntax.webserver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Resources;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import io.onedev.commons.jsyntax.CodeMirrorResource;

public class WebServer extends NanoHTTPD {

	public WebServer() throws IOException {
		super(8080);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
	}

	public static void main(String[] args) {
		try {
			new WebServer();
		} catch (IOException ioe) {
			System.err.println("Couldn't start server:\n" + ioe);
		}
	}
	
	private String readFile(String fileName) {
		try {
			return Resources.toString(Resources.getResource(WebServer.class, fileName), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Response newResponse(String uri, String content) {
		if (uri.endsWith(".css"))
			return newFixedLengthResponse(Status.OK, "text/css", content);
		else if (uri.endsWith(".js"))
			return newFixedLengthResponse(Status.OK, "text/javascript", content);
		else if (uri.endsWith(".html") || uri.equals("/"))
			return newFixedLengthResponse(Status.OK, "text/html", content);
		else
			throw new RuntimeException("Unrecognized uri type: " + uri);
	}
	
	@Override
	public Response serve(IHTTPSession session) {
		if (session.getUri().equals("/")) {
			return newResponse(session.getUri(), readFile("index.html"));
		} else if (session.getUri().startsWith("/index.")) {
			return newResponse(session.getUri(), readFile(session.getUri().substring(1)));
		} else if (session.getUri().startsWith("/addon/") 
				|| session.getUri().startsWith("/lib/") 
				|| session.getUri().startsWith("/mode/")
				|| session.getUri().startsWith("/theme/")) {
			return newResponse(session.getUri(), CodeMirrorResource.readAsString(session.getUri().substring(1)));
		} else {
			return newFixedLengthResponse(Status.NOT_FOUND, "text/html", "Not found");
		}
	}
	
}
