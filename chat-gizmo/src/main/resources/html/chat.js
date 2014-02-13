const server = "10.40.80.57";
const url = "ws://"+server+":8040/websocket";
const userDetailsServiceUrl = "http://"+server+":8040/getUserDetails"; 
const xmppAuthenticationServiceUrl = "http://"+server+":8040/doXmppAuthentication";  
var ws;
var userName;
var logs = new Object();

if ("WebSocket" in window) {
   ws = new WebSocket(url);
} else if ("MozWebSocket" in window) {
   ws = new MozWebSocket(url);
}

$(document).ready(function () {
	
	//get ticket from cookie
	var ticket = $.cookie("ticket");
	userName = $.cookie("membershortname");
	orgName = $.cookie("orgshortname");
	
	if(ticket!=null && ticket!=undefined){
		alert("You have logged into TCC and your ticket is "+ticket)
		//do an ajax request to get the userdetails from the ticket
		var ticketJson = {
			'ticket':ticket
			};
		$.post(userDetailsServiceUrl,ticketJson,function(response){
			if(response.status === '200'){
				alert('Welcome '+response.userName+"@"+response.orgName);
				//do an ajax request to register/login the user to the xmpp server
				xmppLogin(response);
			}
			else{
				alert("There was an error retrieving result from "+userDetailsServiceUrl);
			}
		});
	}
	else if(userName !=null && userName != undefined && orgName != null && orgName != undefined){
		alert('You have cookies set and we identified you as '+userName+"@"+orgName);
		var userDetails = {
				'userName' : userName,
				'orgName' : orgName
		}
		//do an ajax request to register/login the user to the xmpp server
		xmppLogin(userDetails);
	}
	else{
		alert("Your TCC session seems to be old/expired. Please Relogin into TCC");
	}
	
	function xmppLogin(userDetails){
		$.post(xmppAuthenticationServiceUrl,userDetails,function(userDetails){
			if(userDetails.status === '200'){
				console.log(userDetails);
				var data = {
					"Type": "login",
					"Data": {
						"UserName": userDetails.userName,
						"Password": userDetails.password,
						"Server": server
					}	
				}
				console.log("ws connection created");
				ws.send(JSON.stringify(data));
			}
			else{
				alert('auth error!! : '+xmppAuthenticationServiceUrl);
			}
		});
	}
	
	//login using the userdetails
   $("#login").submit(function () {

      console.log("#login submit called");
      userName = $("#username").val();
      var data = {
         "Type": "login",
         "Data": {
            "UserName": userName,
            "Password": $("#password").val(),
            "Server": $("#server").val()
         }
      }

      console.log("ws connection created");

      ws.send(JSON.stringify(data));

      return false;
   });
   
   //handler
   $('input.presenceRadio').click(function(){
	   $('input.presenceRadio').prop('checked','');
	   $(this).prop('checked','checked');
	   console.log('Changed presence to ' + $(this).val());
   });
   
});

function getPresenceColor(presence){
    if (presence == null) {
        color = "#808080"; // gray
     } else if (presence == "away") {
        color = "#FFFF00"; // yellow
     } else if (presence == "available") {
        color = "#00FF00"; // green
     } else if (presence == "busy") {
        color = "#FF0000"; // red
     }
    return color;
}

ws.onmessage = function (event) {
   console.log("Received message:" + event.data);
   var message = JSON.parse(event.data);
   if (message.Type == "presence") {
      for (var i = 0; i < message.Roster.length; i++) {
         var color
         color = getPresenceColor(message.Roster[i].Mode);
         $("select#userlist option[value='" + message.Roster[i].Remote + "']").css("color", color);
      }

   } else if (message.Type == "chat") {
      var remote = $(
         "select#userlist option[value='" + message.Data.Remote + "']")
         .html();
      var text = "<font color=\"red\">(" + remote + ") " + message.Data.Text + "</font><br>";
      if (logs[message.Data.Remote] == undefined) {
         logs[message.Data.Remote] = "";
      }
      logs[message.Data.Remote] += text;
      if ($(":selected").attr("value") == message.Data.Remote) {
         $("div.log").append(text);
      } else {
         $("select#userlist option[value='" + message.Data.Remote + "']")
            .css("background", "#00B9EF");
      }
   } else if (message.Type == "roster") {
      console.log("Roster length : " + message.Roster.length);
      for (var i = 0; i < message.Roster.length; i++) {
         var color
         color = getPresenceColor(message.Roster[i].Mode);
         console.log("added " + message.Roster[i].Name + " to the rosterlist");
         $("select#userlist").append($('<option>').html(message.Roster[i].Name).val(message.Roster[i].Remote).css("color", color));
      }
   } else if (message.Type == "login") {
	   
	   //hide loginBox and display chatform
	   $('#loginBox').remove();
	   $('#chatBox').show();
	   
      $("#chat").submit(function () {
         var message = $("#message").val();
         var data = {
            "Type": "chat",
            "Data": {
               "Remote": $(":selected").attr("value"),
               "Text": message
            }
         };

         ws.send(JSON.stringify(data));
         console.log("Send message:" + JSON.stringify(data));
         userName = (userName===undefined||userName===null)?"You":userName;
         var text = "<font color=\"blue\">(" + userName + ") " + message + "</font><br>";
         if (logs[$(":selected").attr("value")] == undefined) {
            logs[$(":selected").attr("value")] = "";
         }
         logs[$(":selected").attr("value")] += text;
         $("div.log").append(text);
         $("#message").val("");
         return false;
      });

      $("select#userlist").click(function () {
         console.log(logs[$(":selected").attr("value")]);
         $(":selected").css("background", "#FFF");
         if (logs[$(":selected").attr(
            "value")] != undefined) {
            $("div.log").html(logs[$(":selected").attr("value")]);
         } else {
            $("div.log").html("");
         }
      });

   }
};

ws.onclose = function (event) {
   if (event.wasClean) {
      var closed = "Complete";
   } else {
      var closed = "Incompleted";
   }
   console.log("ws connection disconnected:" + closed + ", code:" + event.code + ", reason:" + event.reason);
   alert("Connection disconnected.");
   window.close();
}

// Close connection
window.onunload = function () {
   var code = 4500;
   var reason = "Client closed";
   ws.close(code, reason);
   console.log("ws connection disposed");
}