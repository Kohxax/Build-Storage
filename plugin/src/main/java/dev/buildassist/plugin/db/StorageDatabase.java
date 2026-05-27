package dev.buildassist.plugin.db;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StorageDatabase {

    private final File dataFolder;
    private Connection connection;

    public StorageDatabase(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void initialize() {
        dataFolder.mkdirs();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "storage.db").getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_storage (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT    NOT NULL,
                        item_key    TEXT    NOT NULL,
                        nbt_data    TEXT,
                        count       INTEGER NOT NULL DEFAULT 0,
                        UNIQUE(player_uuid, item_key, nbt_data)
                    )
                """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_uuid ON player_storage(player_uuid)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize storage database", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // ignore on shutdown
        }
    }

    public List<StorageItem> getItems(UUID playerUuid) {
        List<StorageItem> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT item_key, nbt_data, count FROM player_storage WHERE player_uuid = ? AND count > 0")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new StorageItem(rs.getString("item_key"), rs.getString("nbt_data"), rs.getLong("count")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get items for " + playerUuid, e);
        }
        return items;
    }

    public long getCount(UUID playerUuid, String itemKey, String nbtData) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count FROM player_storage WHERE player_uuid = ? AND item_key = ? AND (nbt_data IS ? OR nbt_data = ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, itemKey);
            ps.setString(3, nbtData);
            ps.setString(4, nbtData);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("count");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get count", e);
        }
        return 0;
    }

    public long getTotalCount(UUID playerUuid, String itemKey) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COALESCE(SUM(count), 0) FROM player_storage WHERE player_uuid = ? AND item_key = ? AND count > 0")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, itemKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get total count", e);
        }
        return 0;
    }

    // Adds (positive delta) or removes (negative delta) items. Returns false if insufficient stock.
    public boolean adjustCount(UUID playerUuid, String itemKey, String nbtData, long delta) {
        try {
            if (delta > 0) {
                return deposit(playerUuid, itemKey, nbtData, delta);
            } else {
                return withdraw(playerUuid, itemKey, nbtData, -delta);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to adjust count: " + e.getMessage() + " [item=" + itemKey + "]", e);
        }
    }

    private boolean deposit(UUID playerUuid, String itemKey, String nbtData, long amount) throws SQLException {
        if (nbtData != null) {
            // Non-null nbt_data: SQLite UPSERT handles both insert and update atomically
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO player_storage(player_uuid,item_key,nbt_data,count) VALUES(?,?,?,?) " +
                    "ON CONFLICT(player_uuid,item_key,nbt_data) DO UPDATE SET count=count+excluded.count")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, itemKey);
                ps.setString(3, nbtData);
                ps.setLong(4, amount);
                ps.executeUpdate();
            }
        } else {
            // NULL nbt_data: SQLite UNIQUE treats NULLs as distinct so UPSERT won't merge.
            // Use explicit existence check to avoid creating duplicate null rows.
            boolean exists;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT 1 FROM player_storage WHERE player_uuid=? AND item_key=? AND nbt_data IS NULL LIMIT 1")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, itemKey);
                try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
            }
            if (exists) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE player_storage SET count=count+? WHERE player_uuid=? AND item_key=? AND nbt_data IS NULL")) {
                    ps.setLong(1, amount);
                    ps.setString(2, playerUuid.toString());
                    ps.setString(3, itemKey);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO player_storage(player_uuid,item_key,nbt_data,count) VALUES(?,?,NULL,?)")) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, itemKey);
                    ps.setLong(3, amount);
                    ps.executeUpdate();
                }
            }
        }
        return true;
    }

    private boolean withdraw(UUID playerUuid, String itemKey, String nbtData, long amount) throws SQLException {
        String sql = nbtData != null
            ? "UPDATE player_storage SET count=count-? WHERE player_uuid=? AND item_key=? AND nbt_data=? AND count>=?"
            : "UPDATE player_storage SET count=count-? WHERE player_uuid=? AND item_key=? AND nbt_data IS NULL AND count>=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, itemKey);
            if (nbtData != null) {
                ps.setString(4, nbtData);
                ps.setLong(5, amount);
            } else {
                ps.setLong(4, amount);
            }
            return ps.executeUpdate() > 0;
        }
    }
}
