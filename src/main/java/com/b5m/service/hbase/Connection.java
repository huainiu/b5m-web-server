package com.b5m.service.hbase;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;

import com.b5m.base.common.utils.StringTools;

public class Connection{
    private Configuration hBaseConfig = null;
    private static final Log LOG = LogFactory.getLog(Connection.class);

    static public Connection initConnection(String hbaseZookeeperQuorum, String hbaseZookeeperClientPort) {
        Connection con = new Connection();
        con.hBaseConfig =  HBaseConfiguration.create();
        con.hBaseConfig.set("hbase.zookeeper.quorum", hbaseZookeeperQuorum);
        if(!StringTools.isEmpty(hbaseZookeeperClientPort)){
        	con.hBaseConfig.set("hbase.zookeeper.property.clientPort", hbaseZookeeperClientPort);
        }
        return con;
    }

    public HTable createHTable(String tableName, String columnFamily) {
        HBaseAdmin hAdmin = null;
        boolean tableExists = false;
        boolean tableEnable = false;
        try {
            hAdmin = new HBaseAdmin(hBaseConfig);
            tableExists = hAdmin.tableExists(tableName);
            if (tableExists)
            	tableEnable = hAdmin.isTableEnabled(tableName);
        } catch(IOException e) {
        	e.printStackTrace();
        	LOG.error("ERROR: Access Table Failed, " + e.getCause());
            return null;
        }
        LOG.info("INFO: " + tableName + " is exists ? " + tableExists);
        LOG.info("INFO: " + tableName + " is enabled ? " + tableEnable);
        if (tableExists) {
            HTable hTable = null;
            try {
            	HTableDescriptor desc = hAdmin.getTableDescriptor(tableName.getBytes());
            	HColumnDescriptor[] cds = desc.getColumnFamilies();
            	if(!StringTools.isEmpty(columnFamily)){
            		int i = 0;
            		for (i = 0; i < cds.length; i++) {
            			if (columnFamily.equals(cds[i].getNameAsString()))
            				break;
            		}
            		if (i == cds.length) {
            			hAdmin.disableTable(tableName);
            			//System.out.println(columnFamily);
            			HColumnDescriptor cf = new HColumnDescriptor(columnFamily);
            			cf.setMaxVersions(1);
            			//cf.setBloomFilterType(StoreFile.BloomType.ROW);
            			hAdmin.addColumn(tableName, cf);
            			hAdmin.enableTable(tableName);
            		}
            	}
                if (hAdmin.isTableAvailable(tableName.getBytes()))
                    hTable = new HTable(hBaseConfig, tableName);
                else {
                	LOG.error("INFO: " + tableName + " is not Available");
                }
            }
            catch (IOException e) {
            	LOG.error("ERROR: Access Table Failed, " + e.getCause());
                return null;
            }
            return hTable;
        }
        
        HTable hTable = null;
        try {
            HTableDescriptor desc = new HTableDescriptor(tableName);
            HColumnDescriptor cf = new HColumnDescriptor(columnFamily.getBytes());
            cf.setMaxVersions(1);
            desc.addFamily(cf);
            hAdmin.createTable(desc);
            hAdmin.flush(tableName);
        } catch (IOException e)  {
            System.out.println("ERROR: Create Table Failed, " + e.getCause());
        } catch (InterruptedException e) {}

        try {
            if (hAdmin.isTableAvailable(tableName.getBytes()))
                hTable = new HTable(hBaseConfig, tableName);
            else {
                System.out.println("INFO: " + tableName + " is not Available");
            }
            hAdmin.close();
        } catch (IOException e) {
        	LOG.error("ERROR: Access Table Failed, " + e.getCause());
        }
        return hTable;
    }
}
