all: WebServer.java RequestMessage.java ResponseMessage.java HeaderLine.java MySingleServer.java MyServerService.java
	javac WebServer.java RequestMessage.java ResponseMessage.java HeaderLine.java MySingleServer.java MyServerService.java

clean:
	rm WebServer.class RequestMessage.class ResponseMessage.class HeaderLine.class MySingleServer.class MyServerService.class
