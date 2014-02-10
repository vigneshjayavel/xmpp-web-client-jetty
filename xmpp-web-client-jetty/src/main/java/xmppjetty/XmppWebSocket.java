package xmppjetty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import pojo.Data;
import pojo.Message;
import pojo.User;

public class XmppWebSocket implements WebSocket.OnTextMessage, RosterListener,
		MessageListener, ChatManagerListener {

	protected Connection webSocketConnection;
	protected XMPPConnection xmppConnection;

	public XmppWebSocket() {
		// XMPPConnection.DEBUG_ENABLED = true;
	}

	@Override
	public void onOpen(Connection arg0) {
		this.webSocketConnection = arg0;
	}

	@Override
	public void onClose(int arg0, String arg1) {
		xmppConnection.disconnect();
	}

	@Override
	public void onMessage(String arg0) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Message messageReceived = mapper.readValue(arg0, Message.class);
			if (messageReceived.getType().equals("login")) {

				// Set XMPP connection
				SmackConfiguration.setPacketReplyTimeout(5000);
				ConnectionConfiguration config = new ConnectionConfiguration(
						messageReceived.getData().getServer(), 5222,
						"localhost");
				config.setSASLAuthenticationEnabled(false);

				// Log in to XMPP server
				xmppConnection = new XMPPConnection(config);
				xmppConnection.connect();
				xmppConnection.login(messageReceived.getData().getUserName(),
						messageReceived.getData().getPassword());
				System.out.println(messageReceived.getData().getUserName()
						+ " has logged in!");

				// Send message to frontend
				Message messageToSend = new Message("login");
				webSocketConnection.sendMessage(mapper
						.writeValueAsString(messageToSend));

				// get the rosterlist
				Roster roster = xmppConnection.getRoster();
				roster.addRosterListener(this);
				xmppConnection.getChatManager().addChatListener(this);

				Collection<RosterEntry> entries = roster.getEntries();
				List<User> users = new ArrayList<User>();

				// form the rosterlist
				for (RosterEntry entry : entries) {
					// System.out.println(entry);
					Presence presence = roster.getPresence(entry.getUser());
					String mode = null;
					if (presence.isAvailable()) {
						mode = "available";
					}
					if (presence.isAway()) {
						mode = "away";
					}
					String name = entry.getName() != null ? entry.getName()
							: entry.getUser();
					users.add(new User(name, entry.getUser(), mode));
				}

				Message rosterToSend = new Message("roster", users);

				System.out.println(mapper.writeValueAsString(rosterToSend));

				// send the roster to frontend
				webSocketConnection.sendMessage(mapper
						.writeValueAsString(rosterToSend));

			} else if (messageReceived.getType().equals("chat")) {

				// just route the regular chat messages to the respective
				// receiver
				ChatManager chatManager = xmppConnection.getChatManager();
				Chat chat = chatManager.createChat(messageReceived.getData()
						.getRemote(), this);
				chat.sendMessage(messageReceived.getData().getText());
			}

		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMPPException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		System.out.println(">>chatCreated:" + createdLocally);
		if (!createdLocally) {
			System.out.println(chat.getParticipant());
			chat.addMessageListener(this);
		}

	}

	@Override
	public void processMessage(Chat chat,
			org.jivesoftware.smack.packet.Message messgage) {
		if (messgage.getBody() == null) {
			System.out.println("Empty message body!");
			return;
		}
		StringTokenizer stringTokenizer = new StringTokenizer(
				messgage.getFrom(), "/");
		Message messageToSend = new Message("chat", new Data(
				stringTokenizer.nextToken(), messgage.getBody()));

		ObjectMapper mapper = new ObjectMapper();
		try {
			// send message to the frontend
			webSocketConnection.sendMessage(mapper
					.writeValueAsString(messageToSend));
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void entriesAdded(Collection<String> arg0) {
		System.out.println(">>entriesAdded:");
		Iterator<String> iterator = arg0.iterator();
		while (iterator.hasNext()) {
			String entry = iterator.next();
			System.out.println(entry);
		}
	}

	@Override
	public void entriesDeleted(Collection<String> arg0) {
		System.out.println(">>entriesDeleted:");
		Iterator<String> iterator = arg0.iterator();
		while (iterator.hasNext()) {
			String entry = iterator.next();
			System.out.println(entry);
		}
	}

	@Override
	public void entriesUpdated(Collection<String> arg0) {
		System.out.println(">>entriesUpdated:");
		Iterator<String> iterator = arg0.iterator();
		while (iterator.hasNext()) {
			String entry = iterator.next();
			System.out.println(entry);
		}
	}

	@Override
	public void presenceChanged(Presence presence) {
		System.out.println(">>presenceChanged:" + presence.getFrom() + " "
				+ presence);
		String mode = null;
		if (presence.isAvailable()) {
			mode = "available";
		}
		if (presence.isAway()) {
			mode = "away";
		}
		StringTokenizer stringTokenizer = new StringTokenizer(
				presence.getFrom(), "/");
		User user = new User(null, stringTokenizer.nextToken(), mode);
		List<User> users = new ArrayList<User>();
		users.add(user);
		Message messageToSend = new Message("presence", users);
		ObjectMapper mapper = new ObjectMapper();
		try {
			// send message to the frontend
			webSocketConnection.sendMessage(mapper
					.writeValueAsString(messageToSend));
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
