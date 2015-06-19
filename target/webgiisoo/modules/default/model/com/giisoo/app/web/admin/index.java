/*
 *   WebGiisoo, a java web foramewrok.
 *   Copyright (C) <2014>  <giisoo inc.>
 *
 */
package com.giisoo.app.web.admin;

import com.giisoo.framework.common.*;
import com.giisoo.framework.web.*;

public class index extends Model {

	/* (non-Javadoc)
	 * @see com.giisoo.framework.web.Model#onGet()
	 */
	@Override
	@Require(login = true, access = "access.config.admin")
	public void onGet() {
		/**
		 * let's post method to handle it
		 */
		onPost();
	}

	/* (non-Javadoc)
	 * @see com.giisoo.framework.web.Model#onPost()
	 */
	@Require(login = true, access = "access.config.admin")
	public void onPost() {

		User me = this.getUser();
		/**
		 * put the user in mode
		 */
		this.put("me", me);

		/**
		 * show view ...
		 */
		this.show("/admin/index.html");

	}

}
