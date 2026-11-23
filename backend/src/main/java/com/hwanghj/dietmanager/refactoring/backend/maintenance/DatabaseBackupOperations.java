package com.hwanghj.dietmanager.refactoring.backend.maintenance;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static com.hwanghj.dietmanager.refactoring.backend.maintenance.BackupModel.TABLES;

import com.hwanghj.dietmanager.refactoring.backend.maintenance.BackupModel.Backup;
import com.hwanghj.dietmanager.refactoring.backend.maintenance.BackupModel.Table;

final class DatabaseBackupOperations {
    private DatabaseBackupOperations() {}
    static boolean exists(Connection connection,String table) throws SQLException {
        try(var rs=connection.getMetaData().getTables(connection.getCatalog(),null,table,new String[]{"TABLE"})) { return rs.next(); }
    }
    static Backup snapshot(Connection connection) throws SQLException {
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        connection.setAutoCommit(false);
        var tables=new ArrayList<Table>();
        try {
            for(String name:TABLES) {
                if(!exists(connection,name)) continue;
                String ddl;
                try(var statement=connection.createStatement(); var rs=statement.executeQuery("SHOW CREATE TABLE `"+name+"`")) { rs.next(); ddl=rs.getString(2); }
                var columns=new ArrayList<String>(); var rows=new ArrayList<List<String>>();
                try(var statement=connection.createStatement(); var rs=statement.executeQuery("SELECT * FROM `"+name+"`")) {
                    var meta=rs.getMetaData();
                    for(int i=1;i<=meta.getColumnCount();i++) columns.add(meta.getColumnName(i));
                    while(rs.next()) {
                        var row=new ArrayList<String>();
                        for(int i=1;i<=meta.getColumnCount();i++) row.add(rs.getString(i));
                        rows.add(row);
                    }
                }
                tables.add(new Table(name,ddl,columns,rows));
            }
            connection.commit(); return new Backup(1,tables);
        } finally { connection.rollback(); connection.setAutoCommit(true); }
    }
    static void restoreAndVerify(Connection connection,Backup backup) throws Exception {
        restoreAndVerify(connection,backup,false);
    }
    public static String restoreAndVerify(Connection connection,Backup backup,boolean keep) throws Exception {
        if(backup.format()!=1) throw new IllegalArgumentException("지원하지 않는 백업 형식");
        String prefix="dm_restore_"+UUID.randomUUID().toString().replace("-","").substring(0,12)+"_";
        var created=new ArrayList<String>();
        boolean verified=false;
        try {
            try(var statement=connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                for(Table table:backup.tables()) {
                    if(!TABLES.contains(table.name()) || !table.ddl().startsWith("CREATE TABLE `"+table.name()+"`")) throw new IllegalArgumentException("백업 테이블 확인 실패");
                    String ddl=table.ddl();
                    for(String name:TABLES) ddl=ddl.replace("`"+name+"`","`"+prefix+name+"`");
                    // 외래키 이름은 스키마 내에서 고유해야 한다.
                    ddl=ddl.replaceAll("CONSTRAINT `([^`]+)`", "CONSTRAINT `"+prefix+"$1`");
                    statement.execute(ddl); created.add(prefix+table.name());
                    String placeholders=String.join(",",Collections.nCopies(table.columns().size(),"?"));
                    try(var insert=connection.prepareStatement("INSERT INTO `"+prefix+table.name()+"` VALUES ("+placeholders+")")) {
                        for(var row:table.rows()) {
                            for(int i=0;i<row.size();i++) insert.setString(i+1,row.get(i));
                            insert.addBatch();
                        }
                        insert.executeBatch();
                    }
                }
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
            }
            for(Table expected:backup.tables()) {
                var rows=new ArrayList<List<String>>();
                try(var statement=connection.createStatement(); var rs=statement.executeQuery("SELECT * FROM `"+prefix+expected.name()+"`")) {
                    while(rs.next()) { var row=new ArrayList<String>(); for(int i=1;i<=expected.columns().size();i++) row.add(rs.getString(i)); rows.add(row); }
                }
                // 행 순서에 의존하지 않고 중복까지 보존하여 모든 컬럼 값을 비교한다.
                if(!bag(expected.rows()).equals(bag(rows))) throw new IllegalStateException("복원 내용 불일치: "+expected.name());
            }
            System.out.println("격리된 임시 MySQL 테이블에서 백업 복원 및 전체 행 비교 완료");
            verified=true;
            if(keep) System.out.println("복원 테이블 보존: "+prefix+" (기존 테이블 유지)");
        } finally {
            try(var statement=connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                for(String table:keep && verified ? List.<String>of() : created) {
                    if(!table.startsWith(prefix) || !prefix.matches("dm_restore_[0-9a-f]{12}_")) throw new IllegalStateException("임시 테이블 이름 확인 실패");
                    statement.execute("DROP TABLE `"+table+"`");
                }
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        }
        return prefix;
    }
    private static Map<List<String>,Long> bag(List<List<String>> rows) {
        var result=new HashMap<List<String>,Long>(); for(var row:rows) result.merge(row,1L,Long::sum); return result;
    }
}
