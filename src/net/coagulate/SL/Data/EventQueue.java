package net.coagulate.SL.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** Generic event queue.
 * This class probably doesn't make a lot of sense to most users.
 * It's used for multi-node clusters, it allows a low-effort means of ensuring either node can queue events and only one
 * will dequeue them.
 *
 * This mechanism is only intended to be for singleton resources, e.g. SL bots or Discord bots, stuff that either node
 * can do at any time (e.g. SL Name API) shouldn't be going through this.
 *
 * Dequeued by the maintenance thread, which only runs on the "primary node"
 *
 * TODO: create some way of a node handing over its primary status
 * TODO: there needs to be a cache-disable mode
 */

public class EventQueue extends StandardSLTable{

    public List<EventQueue> getOutstandingEvents() {
        List<EventQueue> set=new ArrayList<>();
        for (ResultsRow row:getDatabase().dq("select * from eventqueue where expires>UNIX_TIMESTAMP() and claimed is null order by id asc")) {
            set.add(new EventQueue(row));
        }
        return set;
    }

    public EventQueue(int id) {
        this(SL.getDB().dqOne("select * from eventqueue where id=?",id));
    }

    private String moduleName;
    private String commandName;
    private JSONObject structuredData;

    public EventQueue(ResultsRow row) {
        super(row.getInt("id"));
        moduleName=row.getString("modulename");
        commandName=row.getString("commandname");
        structuredData=new JSONObject(row.getString("structureddata"));
    }

    @Nonnull
    @Override
    public String getTableName() {
        return "eventqueue";
    }
}
