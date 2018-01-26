package com.galaksiya.wgb.runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.galaksiya.util.data.Board;
import com.galaksiya.util.data.Organization;
import com.galaksiya.util.data.User;
import com.galaksiya.util.data.Workspace;
import com.galaksiya.util.kvstore.KVStoreController;

public class KvStoreController {
	private static KVStoreController kvStore = KVStoreController.getInstance();
	private static User user;

	public static void main(String[] args) throws InterruptedException {

//		 addUser("ali", "veli", "kaansaglam");
		List<User> findUser = kvStore.getUserList();
//		Board board = kvStore.findUser("kaansaglam@galaksiya.com").getBoard("izmir");
//		System.out.println(board.toJsonString());
		// User user = kvStore.findUser("kaansaglam@galaksiya.com");
		// Map<String, Workspace> workspaces = user.getWorkspaces();
		// ArrayList<String> entities =
		// user.getWorkspaces().get(User.DEFAULT_WORKSPACE).getEntities();
		// System.out.println(entities);
		System.out.println(findUser);
		System.exit(0);
	}

	private static void addUser(String... names) throws InterruptedException {
		for (String name : names) {
			user = new User(name, name, name + "@galaksiya.com", name, new Organization("Galaksiya"), "05057305560");
			kvStore.storeUser(user);
			Thread.sleep(2000);
		}
	}
}
