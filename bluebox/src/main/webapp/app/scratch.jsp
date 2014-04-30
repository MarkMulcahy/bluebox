<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.rest.json.JSONMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONRawMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONInboxHandler"%>
<%
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle inboxDetailsResource = ResourceBundle.getBundle("inboxDetails", request.getLocale());
	Config bbconfig = Config.getInstance();
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%=headerResource.getString("welcome")%></title>
	<jsp:include page="dojo.jsp" />	
	<script type="text/javascript" charset="utf-8">
		
		function loadInbox(email, state) {
			require(["dijit/registry"], function(registry){
			    grid = registry.byId("grid");
			    if (grid) {
			    	clearSelection();
			    	store = getStore(email, state);
					grid.setStore(store, {});
			    }
			});	
				
			    
			// set the banner title
			if (email=="")
				document.getElementById("mailTitle").innerHTML = "<%=inboxDetailsResource.getString("allMail")%>";
			else
				document.getElementById("mailTitle").innerHTML = "<%=inboxDetailsResource.getString("inboxfor")%> "+email;
			// set the check fragment
			if (email)
			   	document.getElementById('<%=Inbox.EMAIL%>').value = email;
		   	else
		   		document.getElementById('<%=Inbox.EMAIL%>').value = "";
			currentEmail = email;
			//loadStats();
	
		}
	
		function deleteSelectedRows() {
			var inbox = dijit.byId("grid");
			var items = inbox.selection.getSelected();
			var itemList = "";
			require(["dijit/registry"], function(registry){
			    var grid = registry.byId("grid");
				if(items.length){
					dojo.forEach(items, function(selectedItem){
						if(selectedItem !== null){
							itemList += grid.store.getValue(selectedItem, "<%=BlueboxMessage.UID%>")+",";
						}
					});
					deleteMail(itemList);
					if (items.length>1) {
						inbox.selection.clear();
					}
					loadInboxAndFolder(currentEmail);
				}
				else {
					alert("<%=inboxDetailsResource.getString("error.noselection")%>");
				}
			});
		}
	
		function refresh() {
			loadInboxAndFolder(currentEmail);
		}
	
		function loadAll() {
			loadInboxAndFolder("");
		}
				
		function deleteMail(uidList) {
			if (currentUid) {
				var delUrl = "<%=JSONMessageHandler.JSON_ROOT%>/"+uidList;
				var xhrArgs = {
						url: delUrl,
						handleAs: "text",
						preventCache: true,
						load: function(data) {
							//loadInboxAndFolder(currentEmail);
						},
						error: function (error) {
							alert("<%=inboxDetailsResource.getString("error.unknown")%>"+error);
						}
				};
	
				dojo.xhrDelete(xhrArgs);		
			}
			else {
				alert("<%=inboxDetailsResource.getString("error.noselection")%>");
			}
		}
	
		function clearSelection() {
			require(["dijit/registry"], function(registry){
			    var widget = registry.byId("grid");
			    if (widget)
			    	widget.selection.clear();
		    });
	
		}
		
		function upload() {
			window.location = 'upload.jsp';
		}
		
		function loadRaw() {
			if (currentUid==null) {
				alert("<%=inboxDetailsResource.getString("error.noselection")%>");
			}
			else {
				var load = window.open("../<%=JSONRawMessageHandler.JSON_ROOT%>/"+currentUid,'','scrollbars=yes,menubar=no,height=600,width=800,resizable=yes,toolbar=no,location=no,status=no');
			}
		}
		
		function atomFeed() {
			window.open("atom/inbox?email="+encodeURIComponent(currentEmail));
		}
		
		function getStore(email, state) {
			try {
				var urlStr = "../<%=JSONInboxHandler.JSON_ROOT%>/"+encodeURI(email)+"/"+state;
				 var store = new dojox.data.JsonRestStore({ 
					    				target: urlStr, 
					    				parameters: [{name: "state", type: "string", optional: true}]
					    			    });
				    return store;
			}
			catch (err) {
				alert("Error loading store :"+err);
				return null;
			}
		    
		}
		
		function isOdd(num) {
			return num % 2;
		}
			
		function setupTable(email,state) {
			try {
		      require(["dojox/grid/EnhancedGrid","dojox/data/JsonRestStore","dojox/grid/enhanced/plugins/Pagination","dojox/grid/enhanced/plugins/Selector"], function() {
			    // set the layout structure:
		    	var view = {
					cells: [[
						{name: '<%=inboxDetailsResource.getString("who")%>', field: '<%=BlueboxMessage.FROM%>', width: '20%', editable: false},
						{name: '<%=inboxDetailsResource.getString("subject")%>', field: '<%=BlueboxMessage.SUBJECT%>', width: '55%', editable: false},
						{name: '<%=inboxDetailsResource.getString("date")%>',  field: '<%=BlueboxMessage.RECEIVED%>', width: '15%', editable: false},
						{name: '<%=inboxDetailsResource.getString("size")%>',  field: '<%=BlueboxMessage.SIZE%>', width: '10%', editable: false},
						{name: 'UID',  field: '<%=BlueboxMessage.UID%>', hidden: 'true', editable: false}
					]]
				};
				
				var grid = new dojox.grid.EnhancedGrid({
				      id: 'grid',
				      store: getStore(email, state),
				      structure: view,
				      rowSelector: '0px',
				      plugins:{
				    	   // pagination: {
				    	    	//position: "bottom"
				    	    //},
				    	    selector: {
				    	    	col:"disabled",
				    	    	row:"multi",
				    	    	cell:"disabled"
				    	    }
				    	}
				      });
				grid.placeAt("gridDiv");
				grid.startup();
				
				// connect click events
				dojo.connect(grid, "onEndSelect", function(type, startPoint, endPoint, selected){
					  loadDetail(grid.store.getValue(grid.getItem(endPoint.row), "<%=BlueboxMessage.UID%>"));
					});
				
				// custom style
				//dojo.connect(grid, 'onStyleRow', this, function (row) {
					// needs some work - selected color is being overridden
			      // if (isOdd(row.index)) {
			       //       row.customStyles += "background-color:#ffffaf;";
			      // }
			    //});
				
		      });
			}
			catch (err) {
				alert("setupTable:"+err);
			}
		}
		
		require(["dojo/domReady!","dojox/data/JsonRestStore"], function() {
			// will not be called until DOM is ready
	    	var email = "<%=request.getParameter(Inbox.EMAIL)%>";
			if (email=="null")
				email = "";
			setupTable(email,"<%=BlueboxMessage.State.NORMAL%>");
			loadInbox(email,"<%=BlueboxMessage.State.NORMAL%>");
		});
	</script>		
</head>
<body class="<%=Config.getInstance().getString("dojo_style")%>">
<div id="mailTitle"></div><div id="Email"></div>
<div style="width:100%;height:200px;">
	<div id="gridDiv" style="width:100%;height:400px;"></div>
</div>

</body>
</html>