package com.bluebox.rest.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bluebox.search.SearchIndexer;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONSearchHandler extends AbstractHandler {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String JSON_ROOT = "rest/json/search";

//	protected static String extractSearch(String uri, String fragment) {
//		// /bluebox/rest/json/search/bluemail%20team%20%
//		try {
//			String search;
//			if (uri.endsWith(fragment)) {
//				return "";
//			}
//			else {
//				int pos1 = uri.indexOf(fragment)+fragment.length()+1;
//				int pos2 = uri.lastIndexOf('/');
//				search = uri.substring(pos1,pos2);
//				return URLDecoder.decode(search,"UTF-8");
//			}
//		}
//		catch (Throwable e) {
//			log.severe("No search specified in "+uri);
//			return "";
//		}
//	}
	
	public void doSearchInbox(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		// get the desired email
//		String search = extractSearch(req.getRequestURI(),JSON_ROOT);
		String search = extractFragment(req.getRequestURI(), 1);
		String searchScope = extractFragment(req.getRequestURI(), 2);
		// check sort order, which comes in a strange format "sort(-Subject)=null"
		String orderBy = BlueboxMessage.RECEIVED;
		boolean ascending = false;
		String n;
		for (
		@SuppressWarnings("unchecked")
		Enumeration<String> names = req.getParameterNames(); names.hasMoreElements();) {
			n = names.nextElement();
			if (n.contains("-")) {
				ascending = false;
			}
			if (n.contains(BlueboxMessage.RECEIVED)) {
				orderBy = BlueboxMessage.RECEIVED;
				break;
			}
			if (n.contains(BlueboxMessage.FROM)) {
				orderBy = BlueboxMessage.FROM;
				break;
			}
			if (n.contains(BlueboxMessage.SUBJECT)) {
				orderBy = BlueboxMessage.SUBJECT;
				break;
			}
			if (n.contains(BlueboxMessage.SIZE)) {
				orderBy = BlueboxMessage.SIZE;
				break;
			}			
		}

		// process the paging params "Range: items=0-24"
		String contentHeader = req.getHeader("Range");
		int first = getStart(contentHeader);
		int last = getEnd(contentHeader);
			
		try {
			// tell the grid how many items we have
			log.info("Sending JSON search view for "+search+" first="+first+" last="+last);
			Writer writer = resp.getWriter();
			long totalCount = inbox.searchInbox(search, writer, first, last-first, SearchIndexer.SearchFields.valueOf(searchScope), orderBy, ascending);
			log.info("Total result set was length "+totalCount);
			resp.setHeader("Content-Range", "items "+first+"-"+last+"/"+totalCount);//Content-Range: items 0-24/66
			writer.flush();
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}

	private int getStart(String contentHeader) {
		try {
			// items=0-24, return 0
			int s = contentHeader.indexOf('=')+1;
			int e = contentHeader.indexOf("-", s);
			return Integer.parseInt(contentHeader.substring(s,e));
		}
		catch (Throwable t) {
			log.warning("Invalid Content header :"+contentHeader);
			return 0;
		}
	}

	private int getEnd(String contentHeader) {
		try {
			// items=0-24, return 24
			int s = contentHeader.indexOf('-')+1;
			int e = contentHeader.length();
			return Integer.parseInt(contentHeader.substring(s,e));
		}
		catch (Throwable t) {
			log.warning("Invalid Content header :"+contentHeader);
			return Integer.MAX_VALUE;
		}
	}

}
