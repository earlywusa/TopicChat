<%@ page language="java" session="false" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" trimDirectiveWhitespaces="true" import="java.util.List, chat.Message"%>
<!DOCTYPE html>

<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no"/>
<meta name="description" content="TOPIC CHAT"/>
<title>EA Topic Chat</title>
<style media="screen" type="text/css">

#container {
	max-width: 500px;
	margin: 0px auto;
}

#chat {
	width: 350px;
	height: 200px;
	border: 1px solid silver;
	overflow-y: scroll;
	float: left;
}

#scoreBoard{
	width:190;
	height:200px;
	border: 1px solid silver;
	margin-left: 360px;
}

#msg {
	width: 99%;
	margin-top: 10px;
}

h1 {
	text-align: center;
	font-size: 150%;
}

.center {
	text-align: center;
}


</style>

<script type="text/javascript">
var lastMsgId=0
var messageCount=0;
var userScore = {};

function subscribe(){
	console.log("about to subscribe to url");
	// Check that browser supports EventSource 
	if (!!window.EventSource) {
		// Subscribe to url to listen
		var source = new EventSource('/chat');
	
		// Define what to do when server sent new event
		source.addEventListener("message", function(e) {
			
 			var data = JSON.parse(e.data);
			console.log(JSON.stringify(data)); 
			if(data.topic == getTopic() && data.msg != ""){
				var el = document.getElementById("chat"); 
				if(lastMsgId < data.id){
					el.innerHTML += data.topic + ": " +data.msg + "<br/>";
					el.scrollTop += 50;
					lastMsgId = data.id;
					console.log("last id: " + lastMsgId);
					messageCount ++;
					var userName = data.user;
					var score = data.score;
					if(userName != "heartbeat" && score >= 0){
						userScore[userName] = score;
						displayScore();
					}
				}
			}
		}, false);
	} else {
		alert("Your browser does not support EventSource!");
	}
	
}
function displayScore(){

	var sortArray = sortScore();
	var length = sortArray.length;
	if(length > 10){
		length = 10;
	}
	for(var ind = 0; ind < length; ind ++){
		console.log("name: " + sortArray[ind][0] + " score: " + sortArray[ind][1]);
	}
}

function addToScoreBoard(sortArray){
	var board = document.getElementById('scoreBoard');
	board.innerHTML='';
	
}

function sortScore(){
	var sortArray = [];
	for(var key in userScore){
		console.log("user: " + key + " score: " + userScore[key]);
		sortArray.push([key, userScore[key]]);
	}
	sortArray.sort(function(a,b){
		return b[1] - a[1];
	});
	return sortArray;
}

function clearChatHistory(){
	document.getElementById('chat').innerHTML='';
	lastMsgId=0;
}
function showMsgForm(){
	console.log("show msg form");
	var submitForm = document.getElementById('msgForm');
	submitForm.style.display="block";
}

function getTopic(){
	var topic = document.getElementById('topicField');
	return topic.value;
}

window.onload = function() {
	document.getElementById("chat").scrollTop += 50 * 100;
	document.getElementById("msg").focus();
	subscribe();
};
function prepareHttp(){
	var http = false;
	if (typeof ActiveXObject != "undefined") {
		try {
			http = new ActiveXObject("Msxml2.XMLHTTP");
		} catch (ex) {
			try {
				http = new ActiveXObject("Microsoft.XMLHTTP");
			} catch (ex2) {
				http = false;
			}
		}
	} else if (window.XMLHttpRequest) {
		try {
			http = new XMLHttpRequest();
		} catch (ex) {
			http = false;
		}
	}
	return http;
}

function sendMsg(form) {

	if (form.msg.value.trim() == "") {
		alert("Empty message!");
	}
	
	// Init http object
	var http = prepareHttp();

	if (!http) {
		alert("Unable to connect!");
		return;
	}

	// Prepare data
	var parameters = "msg=" + encodeURIComponent(form.msg.value.trim());

	http.onreadystatechange = function () {
		if (http.readyState == 4 && http.status == 200) {
			if (typeof http.responseText != "undefined") {
				var result = http.responseText;
				form.msg.value = "";
			}
		}
	};

	http.open("POST", form.action, true);
	http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	var topicVal = document.getElementById('topicField').value;
	http.setRequestHeader('topic', topicVal);
	var userName = document.getElementById('userName').value;
	if(userName == '' || userName=="enter your name"){
		userName = "anonymous";
	}
	http.setRequestHeader('user', userName);
	http.send(parameters);

	return false;
}

function loadOldMsg(){
	console.log("chat: " + document.getElementById('chat').innerHTML);
	var topic = getTopic();
	// Init http object
	var http = prepareHttp();

	if (!http) {
		alert("Unable to connect!");
		return;
	}
	/* var form = document.getElementById("msgForm");
	 http.onreadystatechange = function () {
		if (http.readyState == 4 && http.status == 200) {
			if (typeof http.responseText != "undefined") {
				var result = http.responseText;
				form.msg.value = "";
			}
		}
	};  */
	http.open("POST", "/chat", true);
	/* http.setRequestHeader("Content-type", "application/x-www-form-urlencoded"); */
	var topicVal = document.getElementById('topicField').value;
	http.setRequestHeader('topic', topicVal);
	var userName = document.getElementById('userName').value;
	if(userName == '' || userName=="enter your name"){
		userName = "anonymous";
	}
	http.setRequestHeader('user', userName);
	http.setRequestHeader('load', 'all');
	clearChatHistory();
	http.send();
}

</script>

</head>
<body>

<h1>Topic Chat</h1>

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
<p>Ranking:</p>
</div>

<label>Topic: </label>
<input id="topicField" type="text" value="default">
<button type="button" id="btSetTopic" onclick="loadOldMsg()">Set</button>
<br>
<label>Name: </label>
<input id="userName" type="text" value="enter your name"><br>

<form id="msgForm" action="/chat" method="post" onsubmit="return sendMsg(this);" >
	<input type="text" name="msg" id="msg" placeholder="Enter message here"/>
</form>


</div>


</body>
</html>
