/*
 *   WebGiisoo, a java web foramewrok.
 *   Copyright (C) <2014>  <giisoo inc.>
 *
 */
package com.giisoo.app.web;

import com.giisoo.core.bean.X;
import com.giisoo.framework.common.OpLog;
import com.giisoo.framework.common.User;
import com.giisoo.framework.mdc.TConnCenter;
import com.giisoo.framework.web.*;

/**
 * Web 接口: /bye <br/>
 * MDC 客户端断开链接时候，调用改接口
 * 
 * @author joe
 * 
 */
public class bye extends Model {

	/* (non-Javadoc)
	 * @see com.giisoo.framework.web.Model#onMDC()
	 */
	@Override
	@Path(path = X.NONE, method = Model.METHOD_MDC)
	public void onMDC() {
		User u = this.getUser();
		if (u != null) {
			TConnCenter.remove(u.getId());

			OpLog.log("mdc", "bye", "disconnected", u.getId(),
					this.getRemoteHost());
		}
	}

}
