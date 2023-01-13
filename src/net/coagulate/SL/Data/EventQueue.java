package net.coagulate.SL.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.HTML.Elements.Table;
import net.coagulate.Core.Tools.JsonTools;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic event queue.
 * This class probably doesn't make a lot of sense to most users.
 * It's used for multi-node clusters, it allows a low-effort means of ensuring either node can queue events and only one
 * will dequeue them.
 * <p>
 * This mechanism is only intended to be for singleton resources, e.g. SL bots or Discord bots, stuff that either node
 * can do at any time (e.g. SL Name API) shouldn't be going through this.
 * <p>
 * Dequeued by the maintenance thread, which only runs on the "primary node"
 * <p>
 * TODO: create some way of a node handing over its primary status
 * TODO: there needs to be a cache-disable mode
 */

public class EventQueue extends StandardSLTable {
	
	private final String moduleName;
	private final String commandName;
	private final JSONObject structuredData;
	
	public EventQueue(final int id) {
		this(SL.getDB().dqOne("select * from eventqueue where id=?",id));
	}
	
	public EventQueue(final ResultsRow row) {
		super(row.getInt("id"));
		moduleName=row.getString("modulename");
		commandName=row.getString("commandname");
		structuredData=new JSONObject(row.getString("structureddata"));
	}
	
	public static List<EventQueue> getOutstandingEvents() {
		SL.getDB().d("delete from eventqueue where expires<UNIX_TIMESTAMP() and status like 'OK'");
		final List<EventQueue> set=new ArrayList<>();
		for (final ResultsRow row: SL.getDB()
		                             .dq("select * from eventqueue where expires>UNIX_TIMESTAMP() and claimed is null order by id asc")) {
			set.add(new EventQueue(row));
		}
		return set;
	}
	
	public static void queue(final String modulename,
	                         final String commandname,
	                         final int expiresInMinutes,
	                         final JSONObject structureddata) {
		SL.getDB()
		  .d("insert into eventqueue(modulename,commandname,queued,expires,structureddata) values(?,?,?,?,?)",
		     modulename,
		     commandname,
		     UnixTime.getUnixTime(),
		     UnixTime.getUnixTime()+(expiresInMinutes*60),
		     structureddata.toString());
	}
	
	public static Table tabulate() {
		final Table t=new Table();
		t.collapsedBorder();
		t.row()
		 .header("ID")
		 .header("Module")
		 .header("Command")
		 .header("Queued")
		 .header("Expires")
		 .header("Claimed")
		 .header("Completed")
		 .header("Status")
		 .header("Executor")
		 .header("JSON");
		for (final ResultsRow row: SL.getDB().dq("select * from eventqueue order by id desc")) {
			t.row()
			 .add(String.valueOf(row.getInt("id")))
			 .add(row.getString("modulename"))
			 .add(row.getString("commandname"))
			 .add(UnixTime.fromUnixTime(row.getInt("queued"),"Europe/London").replaceAll(" ","&nbsp;"))
			 .add(UnixTime.fromUnixTime(row.getInt("expires"),"Europe/London").replaceAll(" ","&nbsp;"))
			 .add(UnixTime.fromUnixTime(row.getIntNullable("claimed"),"Europe/London").replaceAll(" ","&nbsp;"))
			 .add(UnixTime.fromUnixTime(row.getIntNullable("completed"),"Europe/London").replaceAll(" ","&nbsp;"))
			 .add(row.getString("status"))
			 .add(row.getStringNullable("executor"))
			 .add("<pre>"+JsonTools.jsonToString(new JSONObject(row.getString("structureddata")))+"</pre>");
		}
		return t;
	}
	
	public String getModuleName() {
		return moduleName;
	}
	
	public String getCommandName() {
		return commandName;
	}
	
	public JSONObject getData() {
		return structuredData;
	}
	
	public void claim() {
		set("claimed",UnixTime.getUnixTime());
		set("executor",Config.getHostName());
	}
	
	public void complete() {
		complete("OK");
	}
	
	public void complete(final String status) {
		set("completed",UnixTime.getUnixTime());
		set("status",status);
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "eventqueue";
	}
	
	public void failed() {
		complete("Error");
	}
}
