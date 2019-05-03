package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.SL.SL;

/**
 * @author Iain Price
 */
public abstract class Table extends net.coagulate.Core.Database.Table {

	@Override
	public final DBConnection getDatabase() { return SL.getDB(); }
}
