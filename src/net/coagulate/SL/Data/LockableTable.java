package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.StandardLockableTableRow;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;

/**  Tailored for this env
 *
 * @author Iain Price
 */
public abstract class LockableTable extends StandardLockableTableRow {

    public LockableTable(int id) { super(id); }

    @Override
    public final int getNode() { return Config.getNode(); }

    @Override
    public final DBConnection getDatabase() { return SL.getDB(); }
    
}
