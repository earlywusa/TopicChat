<%@ page language="java" session="false" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" trimDirectiveWhitespaces="true" import="java.util.List, chat.Message"%>
<!DOCTYPE html>

<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no"/>
<meta name="description" content="TOPIC CHAT"/>
<title>EA Topic Chat</title>

<link media="screen" type="text/css" rel="stylesheet" href="${pageContext.request.contextPath}/style.css"/>
<script src="logic.js"></script>

</head>
<body>

<h1>TOPIC CHAT</h1>

<div id="container">

<div id="chat">
<%
	@SuppressWarnings("unchecked")
	List<Message> messages = (List<Message>)request.getAttribute("messages");
	for(Message msg : messages) { %>
		<%= msg.getMessage() %><br/>
<%	}
%>
</div>
<div id="scoreBoard">
</div>

<label>Topic: </label>
<input id="topicField" type="text" placeholder="some topic">
<button type="button" id="btnSetTopic" onclick="loadOldMsg()">Set</button>
<br>
<label>Name: </label>
<input id="userName" type="text" placeholder="enter your name"><br>

<form id="msgForm" action="/chat" method="post" onsubmit="return sendMsg(this);" >
	<input type="text" name="msg" id="msg" placeholder="Enter message here"/>
</form>


</div>


</body>
</html>
