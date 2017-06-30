var lastMsgId=0
var messageCount=0;
var userScore = {};

function subscribe(){
	console.log("about to subscribe to url");
	if (!!window.EventSource) {
		var source = new EventSource('/chat');
	
		source.addEventListener("message", function(e) {
			
 			var data = JSON.parse(e.data);
			console.log(JSON.stringify(data)); 
			if(data.topic == getTopic() && data.msg != ""){
				var el = document.getElementById("chat"); 
				if(lastMsgId < data.id){
					el.innerHTML += data.user + ": " +data.msg + "<br/>";
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
	addToScoreBoard(sortArray);
}

function refreshMessageBoard(jsonArray){
	clearChatHistory();
	console.log("about to refreshMessageBoard");
	var length = jsonArray.length;
	var messageBoard = document.getElementById("chat");
	if(length > 10){
		length = 10;
	}
	for(var index=0; index<length; index++){
		var jsonObj = jsonArray[index];
		console.log("message obj: " + JSON.stringify(jsonObj));
		var user = jsonObj["user"];
		var msg = jsonObj["message"];
		console.log("append: " + user + ":" + msg);
		messageBoard.append(user+":"+msg);
		messageBoard.innerHTML+="<br>";
	}
}

function refreshScoreBoard(jsonArray){
	for(var index in jsonArray){
		console.log("score obj: " + JSON.stringify(jsonArray[index]));
		var user = jsonArray[index]["user"];
		var val = jsonArray[index]["score"];
		userScore[user] = val;
	}
	displayScore();
}

function addToScoreBoard(sortArray){
	var board = document.getElementById('scoreBoard');
	board.innerHTML='';
	var length = sortArray.length;
	if(length > 10){
		length = 10;
	}
	for(var ind = 0; ind < length; ind ++){
		console.log("name: " + sortArray[ind][0] + " score: " + sortArray[ind][1]);
		var ranking = ind +1;
		board.append(ranking + ". " + sortArray[ind][0] + " : " + sortArray[ind][1]);
		board.innerHTML +="<br>";
	}
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
	document.getElementById('scoreBoard').innerHTML='';
	lastMsgId=0;
	userScore={};
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
		return false;
	}
	if(! checkTopicAndUserName()){
		return false;
	}
	
	// Init http object
	var http = prepareHttp();

	if (!http) {
		alert("Unable to connect!");
		return false;
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
	var http = prepareHttp();

	if (!http) {
		alert("Unable to connect!");
		return;
	}
	http.open("POST", "/chat", true);
	if(! checkTopicAndUserName()) {
		return;
	}
	/* http.setRequestHeader("Content-type", "application/x-www-form-urlencoded"); */
	var topicVal = document.getElementById('topicField').value;
	if(topicVal == ''){
		topicVal = "default";
	}
	http.setRequestHeader('topic', topicVal);
	var userName = document.getElementById('userName').value;
	if(userName == '' || userName=="enter your name"){
		userName = "anonymous";
	}
	
	http.onreadystatechange = function () {
		if (http.readyState == 4 && http.status == 200) {
			if (typeof http.responseText != 'undefined') {
				var result = http.responseText;
				console.log("result: " + result);
				var jsonArray = result.split('|');
				var messageArray = [];
				var scoreArray = [];
				for(var i=0; i<jsonArray.length; i++){
					if(jsonArray[i] != ''){
						console.log("to be parse: " + jsonArray[i]);
						var jsonObj = JSON.parse(jsonArray[i]);
						console.log("jsonobj: " + JSON.stringify(jsonObj));
						var score = jsonObj["score"];
						console.log("score val: |" + score + "|");
						if(typeof score != 'undefined' && score != ''){
							scoreArray.push(jsonObj);
						}else{
							console.log("push to message array: " + JSON.stringify(jsonObj));
							messageArray.push(jsonObj);
						}
						
					}
				}
				console.log("message array length: " + messageArray.length);
				refreshMessageBoard(messageArray);
				refreshScoreBoard(scoreArray);
				
			}
		}
	};
	http.setRequestHeader('user', userName);
	http.setRequestHeader('load', 'all');
	clearChatHistory();
	http.send();
}

function checkTopicAndUserName(){
	var topicVal = document.getElementById('topicField').value;
	if(topicVal == "" ){
		alert("Please enter a topic!");
		return false;
	}
	var userName = document.getElementById('userName').value;
	if(userName == ""){
		alert("Please enter a your name!");
		return false;
	}
	return true;
}