var socket;
if (!window.WebSocket) {
	window.WebSocket = window.MozWebSocket;
}

if (window.WebSocket) {
	socket = new WebSocket("ws://localhost:18080/channel");

	socket.onmessage = function(event) {
		var ta = document.getElementById('responseText');
		ta.value = ta.value + '\n' + event.data
	};

	socket.onopen = function(event) {
		var ta = document.getElementById('responseText');
		ta.value = "Web Socket opened!";

		// Regi Message Send 
		if (socket.readyState == WebSocket.OPEN) {
			var payload = {
					"service-name":"svc01",
					"filter":{
						"c1":["100", "101"],
						"c2":["a"]
					}
			};
			socket.send(JSON.stringify(payload));
		}
	};

	socket.onclose = function(event) {
		var ta = document.getElementById('responseText');
		ta.value = ta.value + "Web Socket closed";
	};

} else {
	alert("Your browser does not support Web Socket.");
}

function send(message) {
	if (!window.WebSocket) {
		return;
	}
	if (socket.readyState == WebSocket.OPEN) {
		socket.send(message);
	} else {
		alert("The socket is not open.");
	}
}