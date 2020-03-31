package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.StandardLockableTableRow;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Tailored for this env
 *
 * @author Iain Price
 */
public abstract class LockableTable extends StandardLockableTableRow {

	public LockableTable(final int id) { super(id); }

	// ---------- INSTANCE ----------
	@Override
	public final int getNode() { return Config.getNode(); }

	@Nonnull
	@Override
	public final DBConnection getDatabase() { return SL.getDB(); }

	public Logger logger() { return SL.getLogger(getClass().getSimpleName()); }
}
