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

    // Adds (positive) or removes (negative delta) items. Returns false if insufficient stock.
    public boolean adjustCount(UUID playerUuid, String itemKey, String nbtData, long delta) {
        try {
            connection.setAutoCommit(false);
            long current = getCount(playerUuid, itemKey, nbtData);
            long newCount = current + delta;
            if (newCount < 0) {
                connection.setAutoCommit(true);
                return false;
            }
            if (current == 0 && delta > 0) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO player_storage(player_uuid, item_key, nbt_data, count) VALUES(?,?,?,?)")) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, itemKey);
                    ps.setString(3, nbtData);
                    ps.setLong(4, newCount);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE player_storage SET count = ? WHERE player_uuid = ? AND item_key = ? AND (nbt_data IS ? OR nbt_data = ?)")) {
                    ps.setLong(1, newCount);
                    ps.setString(2, playerUuid.toString());
                    ps.setString(3, itemKey);
                    ps.setString(4, nbtData);
                    ps.setString(5, nbtData);
                    ps.executeUpdate();
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException ignored) {}
            throw new RuntimeException("Failed to adjust count", e);
        }
    }
}
