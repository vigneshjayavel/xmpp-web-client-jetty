const
server = "10.40.76.64";
const
url = "ws://" + server + ":8040/websocket";
const
userDetailsServiceUrl = "http://" + server + ":8040/getUserDetails";
const
xmppAuthenticationServiceUrl = "http://" + server
		+ ":8040/doXmppAuthentication";
var ws;
var userName;
var logs = new Object();

if ("WebSocket" in window) {
	ws = new WebSocket(url);
} else if ("MozWebSocket" in window) {
	ws = new MozWebSocket(url);
}

$(document)
		.ready(
				function() {

					// get ticket from cookie
					var ticket = $.cookie("ticket");
					userName = $.cookie("membershortname");
					orgName = $.cookie("orgshortname");

					if (ticket != null && ticket != undefined) {
						console
								.log("You have logged into TCC and your ticket is "
										+ ticket)
						// do an ajax request to get the userdetails from the
						// ticket
						var ticketJson = {
							'ticket' : ticket
						};
						$
								.post(
										userDetailsServiceUrl,
										ticketJson,
										function(response) {
											if (response.status === '200') {
												console.log('Welcome '
														+ response.userName
														+ "@"
														+ response.orgName);
												// do an ajax request to
												// register/login the user to
												// the xmpp server
												xmppLogin(response);
											} else {
												alert("There was an error retrieving result from "
														+ userDetailsServiceUrl);
											}
										});
					} else if (userName != null && userName != undefined
							&& orgName != null && orgName != undefined) {
						alert('You have cookies set and we identified you as '
								+ userName + "@" + orgName);
						var userDetails = {
							'userName' : userName,
							'orgName' : orgName
						}
						// do an ajax request to register/login the user to the
						// xmpp server
						xmppLogin(userDetails);
					} else {
						alert("Your TCC session seems to be old/expired. Please Relogin into TCC");
					}

					function xmppLogin(userDetails) {
						$
								.post(
										xmppAuthenticationServiceUrl,
										userDetails,
										function(userDetails) {
											if (userDetails.status === '200') {
												console.log(userDetails);
												var data = {
													"Type" : "login",
													"Data" : {
														"UserName" : userDetails.userName,
														"Password" : userDetails.password,
														"Server" : server
													}
												}
												console
														.log("ws connection created");
												ws.send(JSON.stringify(data));
											} else {
												alert('auth error!! : '
														+ xmppAuthenticationServiceUrl);
											}
										});
					}

					// login using the userdetails
					$("#login").submit(function() {

						console.log("#login submit called");
						userName = $("#username").val();
						var data = {
							"Type" : "login",
							"Data" : {
								"UserName" : userName,
								"Password" : $("#password").val(),
								"Server" : $("#server").val()
							}
						}

						console.log("ws connection created");

						ws.send(JSON.stringify(data));

						return false;
					});

					// DOM event handlers
					$("#chat")
							.submit(
									function() {
										var message = $("#message").val();
										var id = $("ul#users li a.active-blue").attr("id");
										var data = {
											"Type" : "chat",
											"Data" : {
												"Remote" : id.replace("-","@"),
												"Text" : message
											}
										};

										ws.send(JSON.stringify(data));
										console.log("Send message:"
												+ JSON.stringify(data));
										userName = (userName === undefined || userName === null) ? "You"
												: userName;
										var text = "<font color=\"blue\">("
												+ userName + ") " + message
												+ "</font><br>";
										if (logs[id] == undefined) {
											logs[id] = "";
										}
										logs[id] += text;
										$("div.log").append(text);
										$("#message").val("");
										return false;
									});

				
					$("ul#users").on("click", "li a", function() {
						$('section#chatForm').show();
						var id = $(this).attr("id")
						$("ul#users li a").removeClass("active-blue");
						$(this).removeClass("active-red");
						$(this).addClass("active-blue");
						console.log(logs[id]);
						if (logs[id] != undefined) {
							$("div.log").html(logs[id]);
						} else {
							$("div.log").html("");
						}
						console.log(id)
					});

					$('input.presenceRadio').click(function() {
						$('input.presenceRadio').prop('checked', '');
						$(this).prop('checked', 'checked');
						var presence = $(this).val();
						// send a ws message to server to update about the
						// status change
						var data = {
							"Type" : "presence",
							"Data" : {
								"Text" : presence
							}
						};
						ws.send(JSON.stringify(data));
						console.log('Changed presence to ' + $(this).val());
					});

				});

function playSound(filename) {
	document.getElementById("sound").innerHTML = '<audio autoplay="autoplay"><source src="assets/'
			+ filename
			+ '.mp3" type="audio/mpeg" /><source src="assets/'
			+ filename
			+ '.ogg" type="audio/ogg" /><embed hidden="true" autostart="true" loop="false" src="assets/'
			+ filename + '.mp3" /></audio>';
}

function getPresenceColor(presence) {
	if (presence == null) {
		color = "gray"; // gray
	} else if (presence == "away") {
		color = "orange"; // yellow
	} else if (presence == "available" || presence == "chat") {
		color = "green"; // green
	} else if (presence == "dnd") {
		color = "red"; // red
	}
	return color;
}

// handling messages from server
ws.onmessage = function(event) {
	console.log("Received message:" + event.data);
	var message = JSON.parse(event.data);
	if (message.Type == "presence") {
		for (var i = 0; i < message.Roster.length; i++) {
			var color
			color = getPresenceColor(message.Roster[i].Mode);
			console.log(message.Roster[i].Remote + " is changed to "
					+ message.Roster[i].Mode);
			var id = (message.Roster[i].Remote).replace("@", "-");
			var select = 'ul#users li a span#' + id;
			$(select).removeClass("orange green gray red").addClass(color);
		}

	} else if (message.Type == "chat") {
		var remote = (message.Data.Remote).replace("@","-");
		var text = "<font color=\"red\">(" + remote.substring(0,remote.indexOf("-")) + ") " + message.Data.Text
				+ "</font><br>";
		if (logs[remote] == undefined) {
			logs[remote] = "";
		}
		logs[remote] += text;
		if ($("ul#users li a.active-blue").attr("id") === remote) {
			$("div.log").append(text);
		} else {
			$("ul#users li a#" + remote).addClass("active-red");
			playSound('ping');
		}
	} else if (message.Type == "roster") {
		console.log("Roster length : " + message.Roster.length);
		for (var i = 0; i < message.Roster.length; i++) {
			var color
			color = getPresenceColor(message.Roster[i].Mode);
			console.log("added " + message.Roster[i].Name
					+ " to the rosterlist");
			var id = (message.Roster[i].Remote).replace("@", "-");
			$("ul#users")
					.append(
							'<li><a id="'
									+ id
									+ '" href="#"><span class="glyphicon glyphicon-user '
									+ color + '" id="' + id + '"></span> '
									+ message.Roster[i].Name + ' </a></li>');
		}
	} else if (message.Type == "login") {

		// hide loginBox and display chatform
		$('#loginBox').remove();
		$('#chatBox').show();

	}
};

ws.onclose = function(event) {
	if (event.wasClean) {
		var closed = "Complete";
	} else {
		var closed = "Incompleted";
	}
	console.log("ws connection disconnected:" + closed + ", code:" + event.code
			+ ", reason:" + event.reason);
	alert("Connection disconnected.");
	window.close();
}

// Close connection
window.onunload = function() {
	var code = 4500;
	var reason = "Client closed";
	ws.close(code, reason);
	console.log("ws connection disposed");
}
