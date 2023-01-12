package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public abstract class Table extends net.coagulate.Core.Database.Table {
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public final DBConnection getDatabase() {
		return SL.getDB();
	}
}
