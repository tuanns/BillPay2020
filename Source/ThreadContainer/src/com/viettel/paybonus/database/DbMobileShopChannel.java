/*
 * Copyright 2012 Viettel Telecom. All rights reserved.
 * VIETTEL PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.viettel.paybonus.database;

import com.viettel.cluster.agent.integration.Record;
import com.viettel.threadfw.manager.AppManager;
import com.viettel.paybonus.obj.*;
import com.viettel.threadfw.database.DbProcessorAbstract;
import com.viettel.vas.util.PoolStore;
import com.viettel.vas.util.obj.DataResources;
import com.viettel.vas.util.obj.Param;
import com.viettel.vas.util.obj.ParamList;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 *
 * Thong tin phien ban
 *
 * @author HuyNQ13
 * @version 1.0
 * @since 24-03-2016
 */
public class DbMobileShopChannel extends DbProcessorAbstract {

    private String loggerLabel = DbMobileShopChannel.class.getSimpleName() + ": ";
    private PoolStore poolStore;
    private String dbNameCofig;

    public DbMobileShopChannel() throws SQLException, Exception {
        this.logger = Logger.getLogger(loggerLabel);
        dbNameCofig = ResourceBundle.getBundle("configPayBonus").getString("dbNameConfig");
        poolStore = new PoolStore(dbNameCofig, logger);
    }

    public DbMobileShopChannel(String sessionName, Logger logger) throws SQLException, Exception {
        this.logger = logger;
        dbNameCofig = sessionName;
        poolStore = new PoolStore(sessionName, logger);
    }

    public void closeStatement(Statement st) {
        try {
            if (st != null) {
                st.close();
                st = null;
            }
        } catch (Exception ex) {
            st = null;
        }
    }

    public void logTimeDb(String strLog, long timeSt) {
        long timeEx = System.currentTimeMillis() - timeSt;

        if (timeEx >= AppManager.minTimeDb && AppManager.loggerDbMap != null) {
            br.setLength(0);
            br.append(loggerLabel).
                    append(AppManager.getTimeLevelDb(timeEx)).append(": ").
                    append(strLog).
                    append(": ").
                    append(timeEx).
                    append(" ms");

            logger.warn(br);
        } else {
            br.setLength(0);
            br.append(loggerLabel).
                    append(strLog).
                    append(": ").
                    append(timeEx).
                    append(" ms");

            logger.info(br);
        }
    }

    @Override
    public Record parse(ResultSet rs) {
        MobileShopChannel record = new MobileShopChannel();
        long timeSt = System.currentTimeMillis();
        try {
            record.setId(rs.getLong("action_audit_id"));
            record.setActionAuditId(rs.getLong("action_audit_id"));
            record.setIsdn(rs.getString("isdn"));
            record.setCreateTime(rs.getTimestamp("create_time"));
            record.setCreateStaff(rs.getString("CREATE_STAFF"));
            record.setCheckInfo(rs.getString("check_info"));
            record.seteMolaIsdn(rs.getString("EMOLA_ISDN"));
            record.setItemFeeId(rs.getLong("item_fee_id"));
            record.setFirstAmount(rs.getLong("first_amount"));
            record.setSecondAmount(rs.getLong("second_amount"));
            record.setCountProcess(rs.getLong("count_process"));
            record.setLastProcess(rs.getTimestamp("last_process"));
            record.setChannelTypeId(rs.getInt("CHANNEL_TYPE_ID"));
            record.setActionCode(rs.getString("ACTION_CODE"));
            record.setResultCode("0");
            record.setDescription("Start Processing");
        } catch (Exception ex) {
            logger.error("ERROR parse SubProfileInfo");
            logger.error(AppManager.logException(timeSt, ex));
        }
        return record;
    }

    @Override
    public int[] deleteQueue(List<Record> listRecords) {
        return new int[0];
    }

    @Override
    public int[] insertQueueHis(List<Record> listRecords) {
        return new int[0];
    }

    @Override
    public int[] insertQueueOutput(List<Record> listRecords) {
        return new int[0];
    }

    @Override
    public int[] updateQueueInput(List<Record> listRecords) {
        return new int[0];
    }

    @Override
    public void processTimeoutRecord(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        try {
//            The first delete queue timeout
            deleteQueueTimeout(ids);
//            Save history
            for (String sd : ids) {
                sb.append(": ").append(sd);
            }
            logger.warn("Dispatcher not get reponse from agent, so processTimeoutRecord ID " + sb.toString());
        } catch (Exception ex) {
            logger.error("ERROR processTimeoutRecord ID " + sb.toString() + " " + ex.toString());
        }
    }

    public long updateBounesMonthly(BounesMonthly bn) {
        Connection connection = null;
        PreparedStatement ps = null;
        StringBuilder br = new StringBuilder();
        String sql = "";
        int res = 0;
        long startTime = System.currentTimeMillis();
        try {
            connection = getConnection(dbNameCofig);
            sql = "UPDATE bounes_monthly SET count_process = ? WHERE action_audit_id = ? ";
            ps = connection.prepareStatement(sql);
            ps.setLong(1, bn.getCountProcess() - 1);
            ps.setLong(2, bn.getActionAuditId());
            res = ps.executeUpdate();
            logger.info("End updateBounesMonthly id " + bn.getActionAuditId() + " isdn " + bn.getIsdn()
                    + " time " + (System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            br.setLength(0);
            br.append(loggerLabel).append(new Date()).
                    append("\nERROR updateBounesMonthly id " + bn.getActionAuditId() + " isdn " + bn.getIsdn());
            logger.error(AppManager.logException(startTime, ex));
            res = 0;
        } finally {
            closeStatement(ps);
            closeConnection(connection);
            return res;
        }
    }

    public long updateMobileShopChannel(MobileShopChannel bn) {
        Connection connection = null;
        PreparedStatement ps = null;
        StringBuilder br = new StringBuilder();
        String sql = "";
        int res = 0;
        long startTime = System.currentTimeMillis();
        try {
            connection = getConnection(dbNameCofig);
            sql = "UPDATE bonus_mobile_shop SET count_process = ?, last_process = sysdate,result_code = ?,description = ?,duration = ? WHERE action_audit_id = ? ";
            ps = connection.prepareStatement(sql);
            ps.setLong(1, bn.getCountProcess());
            ps.setString(2, bn.getResultCode());
            ps.setString(3, bn.getDescription());
            ps.setLong(4, bn.getDuration());
            ps.setLong(5, bn.getActionAuditId());
            res = ps.executeUpdate();
            logger.info("End updatePayMobileShopChannel id " + bn.getActionAuditId() + " isdn " + bn.getIsdn()
                    + " time " + (System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            br.setLength(0);
            br.append(loggerLabel).append(new Date()).
                    append("\nERROR updatePayMobileShopChannel id " + bn.getActionAuditId() + " isdn " + bn.getIsdn());
            logger.error(AppManager.logException(startTime, ex));
            res = 0;
        } finally {
            closeStatement(ps);
            closeConnection(connection);
            return res;
        }
    }

    @Override
    public void updateSqlMoParam(List<Record> lrc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean checkAlreadyProcessRecord(long idRecord) {
        ParamList paramList = new ParamList();
        boolean result = false;
        long timeSt = System.currentTimeMillis();
        try {
            paramList.add(new Param("action_audit_id", idRecord, Param.DataType.LONG, Param.IN));
            paramList.add(new Param("action_audit_id", null, Param.DataType.LONG, Param.OUT));
            DataResources rs = poolStore.selectTable(paramList, "bonus_mobile_shop_his");
            while (rs.next()) {
                long id = rs.getLong("action_audit_id");
                if (id > 0) {
                    result = true;
                    break;
                }
            }
            logTimeDb("Time to checkAlreadyProcessRecord idRecord " + idRecord + " result: " + result, timeSt);
        } catch (Exception ex) {
            logger.error("ERROR checkAlreadyProcessRecord defaul return false" + idRecord);
            logger.error(AppManager.logException(timeSt, ex));
            result = false;
        }
        return result;
    }

    public int[] deleteQueueTimeout(List<String> listId) {
        return new int[0];
    }

    public int sendSms(String msisdn, String message, String channel) {
        Connection connection = null;
        PreparedStatement ps = null;
        StringBuilder br = new StringBuilder();
        String sql = "";
        int result = 0;
        long startTime = System.currentTimeMillis();
        try {
            connection = getConnection("dbapp2");
            sql = "INSERT INTO mt (mt_id,msisdn,message,mo_his_id,retry_num,receive_time,channel) "
                    + "VALUES(mt_SEQ.nextval,?,?,null,0,sysdate,?)";
            ps = connection.prepareStatement(sql);
            if (!msisdn.startsWith("258")) {
                msisdn = "258" + msisdn;
            }
            ps.setString(1, msisdn);
            ps.setString(2, message);
            ps.setString(3, channel);
            result = ps.executeUpdate();
            logger.info("End sendSms isdn " + msisdn + " message " + message + " result " + result + " time "
                    + (System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            br.setLength(0);
            br.append(loggerLabel).append(new Date()).
                    append("\nERROR sendSms: ").
                    append(sql).append("\n")
                    .append(" isdn ")
                    .append(msisdn)
                    .append(" message ")
                    .append(message)
                    .append(" result ")
                    .append(result);
            logger.error(br + ex.toString());
        } finally {
            closeStatement(ps);
            closeConnection(connection);
            return result;
        }
    }

    public int insertEwalletLog(EwalletLog log) {

        ParamList paramList = new ParamList();
        long timeSt = System.currentTimeMillis();
        try {
            paramList.add(new Param("ACTION_AUDIT_ID", Long.valueOf(log.getAtionAuditId()), Param.DataType.LONG, Param.IN));
            paramList.add(new Param("STAFF_CODE", log.getStaffCode(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("CHANNEL_TYPE_ID", log.getChannelTypeId(), Param.DataType.INT, Param.IN));
            paramList.add(new Param("MOBILE", log.getMobile(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("TRANS_ID", log.getTransId(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("ACTION_CODE", log.getActionCode(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("AMOUNT", log.getAmount(), Param.DataType.LONG, Param.IN));
            paramList.add(new Param("FUNCTION_NAME", log.getFunctionName(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("URL", log.getUrl(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("USERNAME", log.getUserName(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("REQUEST", log.getRequest(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("RESPONSE", log.getRespone(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("DURATION", log.getDuration(), Param.DataType.LONG, Param.IN));
            paramList.add(new Param("ERROR_CODE", log.getErrorCode(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("DESCRIPTION", log.getDescription(), Param.DataType.STRING, Param.IN));
            paramList.add(new Param("BONUS_TYPE", 6L, Param.DataType.LONG, Param.IN));
            PoolStore.PoolResult prs = poolStore.insertTable(paramList, "EWALLET_LOG");
            logTimeDb("Time to insertEwalletLog isdn " + log.getMobile(), timeSt);
            return prs == PoolStore.PoolResult.SUCCESS ? 0 : -1;
        } catch (Exception ex) {
            logger.error("ERROR insertEwalletLog default return -1: isdn " + log.getMobile());
            logger.error(AppManager.logException(timeSt, ex));
            return -1;
        }
    }

    public int insertBounesMonth(MobileShopChannel bn, String toIMEI) {
        Connection connection = null;
        PreparedStatement ps = null;
        StringBuilder br = new StringBuilder();
        String sql = "";
        int result = 0;
        long startTime = System.currentTimeMillis();
        try {
            connection = getConnection(dbNameCofig);
            sql = "INSERT into Bounes_Monthly(ACTION_AUDIT_ID,ISDN,CREATE_TIME,CREATE_STAFF,CHECK_INFO,EMOLA_ISDN,COUNT_PROCESS,LAST_TIME,IMEI,result_code,description,duration) VALUES(?,?,?,?,?,?,?,sysdate,?,null,null,null)";
            ps = connection.prepareStatement(sql);
            ps.setLong(1, bn.getActionAuditId());
            ps.setString(2, bn.getIsdn());
            ps.setTimestamp(3, new Timestamp(bn.getCreateTime().getTime()));
            ps.setString(4, bn.getCreateStaff());
            ps.setString(5, bn.getCheckInfo());
            ps.setString(6, bn.geteMolaIsdn());
            ps.setLong(7, 33);
            ps.setString(8, toIMEI);

            result = ps.executeUpdate();
            logger.info("End Bounes_Monthly id " + bn.getId() + " isdn " + bn.getIsdn() + " result " + result + " time "
                    + (System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            br.setLength(0);
            br.append(loggerLabel).append(new Date()).
                    append("\nERROR Bounes_Monthly: ").
                    append(sql).append("\n")
                    .append(" id ")
                    .append(bn.getId())
                    .append(" isdn ")
                    .append(bn.getIsdn())
                    .append(" result ")
                    .append(result);
            logger.error(br + ex.toString());
        } finally {
            closeStatement(ps);
            closeConnection(connection);
            return result;
        }
    }

    public int insertMobileShopChannelHis(MobileShopChannel bn, String imei) {
        Connection connection = null;
        PreparedStatement ps = null;
        StringBuilder br = new StringBuilder();
        String sql = "";
        int result = 0;
        long startTime = System.currentTimeMillis();
        try {
            connection = getConnection(dbNameCofig);
            sql = "INSERT INTO bonus_mobile_shop_his (ACTION_AUDIT_ID,ISDN,CREATE_STAFF,CHECK_INFO,EMOLA_ISDN,ITEM_FEE_ID,"
                    + "FIRST_AMOUNT,SECOND_AMOUNT, second_pay_card, result_code, "
                    + "ewallet_error_code, description, count_process, node_name, cluster_name, duration, channel_type_id, action_code,"
                    + "SECOND_PAY_TIME, create_time,imei) \n"
                    + " VALUES(?,?,?,?,?,?,?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";
            ps = connection.prepareStatement(sql);
            ps.setLong(1, bn.getActionAuditId());
            ps.setString(2, bn.getIsdn());
            ps.setString(3, bn.getCreateStaff());
            ps.setString(4, bn.getCheckInfo());
            ps.setString(5, bn.geteMolaIsdn());
            ps.setLong(6, bn.getItemFeeId());
            ps.setLong(7, bn.getFirstAmount());
            ps.setLong(8, bn.getSecondAmount());
            ps.setString(9, bn.getSecondPayCard());
            ps.setString(10, bn.getResultCode());
            ps.setString(11, bn.geteWalletErrCode());
            ps.setString(12, bn.getDescription());
            ps.setLong(13, bn.getCountProcess());
            ps.setString(14, bn.getNodeName());
            ps.setString(15, bn.getClusterName());
            ps.setLong(16, bn.getDuration());
            ps.setInt(17, bn.getChannelTypeId());
            ps.setString(18, bn.getActionCode());
            if (bn.getSecondPayTime() == null) {
                ps.setTimestamp(19, null);
            } else {
                ps.setTimestamp(19, new Timestamp(bn.getSecondPayTime().getTime()));
            }
            ps.setTimestamp(20, new Timestamp(bn.getCreateTime().getTime()));
            ps.setString(21, imei);
            result = ps.executeUpdate();
            logger.info("End insertMobileShopChannelHis id " + bn.getId() + " isdn " + bn.getIsdn() + " result " + result + " time "
                    + (System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            br.setLength(0);
            br.append(loggerLabel).append(new Date()).
                    append("\nERROR insertMobileShopChannelHis: ").
                    append(sql).append("\n")
                    .append(" id ")
                    .append(bn.getId())
                    .append(" isdn ")
                    .append(bn.getIsdn())
                    .append(" result ")
                    .append(result);
            logger.error(br + ex.toString());
        } finally {
            closeStatement(ps);
            closeConnection(connection);
            return result;
        }
    }

    public int deleteMobileShopChannel(MobileShopChannel bn) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        int res = 0;
        try {
            connection = connection = getConnection(dbNameCofig);
            ps = connection.prepareStatement("delete bonus_mobile_shop where action_audit_id = ?");
            ps.setLong(1, bn.getActionAuditId());
            res = ps.executeUpdate();
        } catch (Exception ex) {
            logger.error("ERROR deleteMobileShopChannel getActionAuditId " + bn.getActionAuditId() + " isdn " + bn.getIsdn(), ex);
            logger.error(AppManager.logException(timeStart, ex));
        } finally {
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to deleteMobileShopChannel, getActionAuditId " + bn.getActionAuditId() + " isdn " + bn.getIsdn() + " result " + res, timeStart);
            return res;
        }
    }

    public String[] checkScraftCard(MobileShopChannel bn, String backDay) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String[] res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("cm_pre");
            ps = connection.prepareStatement("SELECT   seri_number, refill_amount, refill_date\n"
                    + "  FROM   cm_pre.mc_scratch_history\n"
                    + " WHERE       refill_date >= TRUNC (SYSDATE - ?)\n"
                    + "         AND refill_date < TRUNC (SYSDATE - ?)\n"
                    + "         AND refill_isdn = ? AND refill_amount >=20");
            ps.setString(1, backDay);
            ps.setString(2, String.valueOf(Integer.valueOf(backDay) - 1));
            ps.setString(3, bn.getIsdn());
            rs = ps.executeQuery();
            if (rs.next()) {
                String serial = rs.getString("seri_number");
                String amount = rs.getString("refill_amount");
                String refilDate = sdf.format(rs.getDate("refill_date"));
                res = new String[]{serial, amount, refilDate};
            }
        } catch (Exception ex) {
            logger.error("ERROR checkScraftCard id " + bn.getId() + " isdn " + bn.getIsdn() + " backDay " + backDay, ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkScraftCard, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public String[] checkTopup(MobileShopChannel bn, String backDay) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String[] res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("appBccsGw");
            ps = connection.prepareStatement("SELECT   client, start_date, money "
                    + "  FROM   topup_log\n"
                    + " WHERE       start_date >= TRUNC (SYSDATE - ?)\n"
                    + "         AND start_date < TRUNC (SYSDATE - ?)\n"
                    + "         AND isdn = ?\n"
                    + "         AND result_code = '0' AND money >= 20");
            ps.setString(1, backDay);
            ps.setString(2, String.valueOf(Integer.valueOf(backDay) - 1));
            ps.setString(3, bn.getIsdn());
            rs = ps.executeQuery();
            if (rs.next()) {
                String client = rs.getString("client");
                String amount = rs.getString("money");
                String refilDate = sdf.format(rs.getDate("start_date"));
                res = new String[]{client, amount, refilDate};
            }
        } catch (Exception ex) {
            logger.error("ERROR checkTopup id " + bn.getId() + " isdn " + bn.getIsdn() + " backDay " + backDay, ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkTopup, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public String[] checkTopupPartner1(MobileShopChannel bn, String backDay) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String[] res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("recarg_aki");
            ps = connection.prepareStatement("SELECT   client, start_date, money "
                    + "  FROM   topup_log\n"
                    + " WHERE       start_date >= TRUNC (SYSDATE - ?)\n"
                    + "         AND start_date < TRUNC (SYSDATE - ?)\n"
                    + "         AND isdn = ?\n"
                    + "         AND result_code = '0' AND money >= 20");
            ps.setString(1, backDay);
            ps.setString(2, String.valueOf(Integer.valueOf(backDay) - 1));
            ps.setString(3, bn.getIsdn());
            rs = ps.executeQuery();
            if (rs.next()) {
                String client = rs.getString("client");
                String amount = rs.getString("money");
                String refilDate = sdf.format(rs.getDate("start_date"));
                res = new String[]{client, amount, refilDate};
            }
        } catch (Exception ex) {
            logger.error("ERROR checkTopupPartner1 id " + bn.getId() + " isdn " + bn.getIsdn() + " backDay " + backDay, ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkTopupPartner1, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public String[] checkTopupPartner2(MobileShopChannel bn, String backDay) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String[] res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("UTTM");
            ps = connection.prepareStatement("SELECT   client, start_date, money "
                    + "  FROM   topup_log\n"
                    + " WHERE       start_date >= TRUNC (SYSDATE - ?)\n"
                    + "         AND start_date < TRUNC (SYSDATE - ?)\n"
                    + "         AND isdn = ?\n"
                    + "         AND result_code = '0' AND money >= 20");
            ps.setString(1, backDay);
            ps.setString(2, String.valueOf(Integer.valueOf(backDay) - 1));
            ps.setString(3, "258" + bn.getIsdn());
            rs = ps.executeQuery();
            if (rs.next()) {
                String client = rs.getString("client");
                String amount = rs.getString("money");
                String refilDate = sdf.format(rs.getDate("start_date"));
                res = new String[]{client, amount, refilDate};
            }
        } catch (Exception ex) {
            logger.error("ERROR checkTopupPartner2 id " + bn.getId() + " isdn " + bn.getIsdn() + " backDay " + backDay, ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkTopupPartner2, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public String[] checkScraftCardToDay(MobileShopChannel bn) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String[] res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("cm_pre");
            ps = connection.prepareStatement("SELECT   seri_number, refill_amount, refill_date\n"
                    + "  FROM   cm_pre.mc_scratch_history\n"
                    + " WHERE       refill_date >= TRUNC (SYSDATE)\n"
                    + "         AND refill_isdn = ? AND refill_amount >=20 ");
            ps.setString(1, bn.getIsdn());
            rs = ps.executeQuery();
            while (rs.next()) {
                String serial = rs.getString("seri_number");
                String amount = rs.getString("refill_amount");
                String refilDate = sdf.format(rs.getDate("refill_date"));
                res = new String[]{serial, amount, refilDate};
            }
        } catch (Exception ex) {
            logger.error("ERROR checkScraftCardToDay id " + bn.getId() + " isdn " + bn.getIsdn(), ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkScraftCardToDay, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public String[] checkTopupToday(MobileShopChannel bn) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String[] res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("appBccsGw");
            ps = connection.prepareStatement("SELECT   client, start_date, money "
                    + "  FROM   topup_log\n"
                    + " WHERE       start_date >= TRUNC (SYSDATE)\n"
                    + "         AND isdn = ?\n"
                    + "         AND result_code = '0' AND money >= 20 ");
            ps.setString(1, bn.getIsdn());
            rs = ps.executeQuery();
            if (rs.next()) {
                String client = rs.getString("client");
                String amount = rs.getString("money");
                String refilDate = sdf.format(rs.getDate("start_date"));
                res = new String[]{client, amount, refilDate};
            }
        } catch (Exception ex) {
            logger.error("ERROR checkTopupToday id " + bn.getId() + " isdn " + bn.getIsdn(), ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkTopupToday, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public String[] checkTopupTodayPartner1(MobileShopChannel bn) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String[] res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("recarg_aki");
            ps = connection.prepareStatement("SELECT   client, start_date, money "
                    + "  FROM   topup_log\n"
                    + " WHERE       start_date >= TRUNC (SYSDATE)\n"
                    + "         AND isdn = ?\n"
                    + "         AND result_code = '0' AND money >= 20 ");
            ps.setString(1, bn.getIsdn());
            rs = ps.executeQuery();
            if (rs.next()) {
                String client = rs.getString("client");
                String amount = rs.getString("money");
                String refilDate = sdf.format(rs.getDate("start_date"));
                res = new String[]{client, amount, refilDate};
            }
        } catch (Exception ex) {
            logger.error("ERROR checkTopupTodayPartner1 id " + bn.getId() + " isdn " + bn.getIsdn(), ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkTopupTodayPartner1, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public String[] checkTopupTodayPartner2(MobileShopChannel bn) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String[] res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("UTTM");
            ps = connection.prepareStatement("SELECT   client, start_date, money "
                    + "  FROM   topup_log\n"
                    + " WHERE       start_date >= TRUNC (SYSDATE)\n"
                    + "         AND isdn = ?\n"
                    + "         AND result_code = '0' AND money >= 20 ");
            ps.setString(1, "258" + bn.getIsdn());
            rs = ps.executeQuery();
            if (rs.next()) {
                String client = rs.getString("client");
                String amount = rs.getString("money");
                String refilDate = sdf.format(rs.getDate("start_date"));
                res = new String[]{client, amount, refilDate};
            }
        } catch (Exception ex) {
            logger.error("ERROR checkTopupTodayPartner2 id " + bn.getId() + " isdn " + bn.getIsdn(), ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkTopupTodayPartner2, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public String checkNewHandset(MobileShopChannel bn, String isdn, String to_imei) {
        long timeStart = System.currentTimeMillis();
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
        String res = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            connection = getConnection("mdm");
            ps = connection.prepareStatement("select msisdn from hlr_subscriber_cache where  datetime >= trunc(sysdate-180) and imei = ?  ");
            ps.setString(1, to_imei);
            rs = ps.executeQuery();
            if (rs.next()) {
                String rsimei = rs.getString("msisdn");
                res = rsimei;
            }
        } catch (Exception ex) {
            logger.error("ERROR checkNewHandset id " + bn.getId() + " isdn " + bn.getIsdn(), ex);
            logger.error(AppManager.logException(timeStart, ex));
            res = null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(connection);
            logTimeDb("Time to checkNewHandset, id " + bn.getId() + " isdn " + bn.getIsdn(), timeStart);
            return res;
        }
    }

    public long updateBonusProces(MobileShopChannel bn) {
        Connection connection = null;
        PreparedStatement ps = null;
        StringBuilder br = new StringBuilder();
        String sql = "";
        int res = 0;
        long startTime = System.currentTimeMillis();
        try {
            connection = getConnection(dbNameCofig);
            sql = "UPDATE bonus_process SET second_pay_amount = ?, second_pay_time = sysdate, second_pay_card = ? "
                    + " WHERE date_process > trunc(sysdate-60) and action_audit_id = ? ";
            ps = connection.prepareStatement(sql);
            ps.setLong(1, bn.getSecondAmount());
            ps.setString(2, bn.getSecondPayCard());
            ps.setLong(3, bn.getActionAuditId());
            res = ps.executeUpdate();
            logger.info("End updateBonusProces id " + bn.getActionAuditId() + " isdn " + bn.getIsdn() + " res " + res
                    + " time " + (System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            br.setLength(0);
            br.append(loggerLabel).append(new Date()).
                    append("\nERROR updateBonusProces id " + bn.getActionAuditId() + " isdn " + bn.getIsdn());
            logger.error(AppManager.logException(startTime, ex));
            res = 0;
        } finally {
            closeStatement(ps);
            closeConnection(connection);
            return res;
        }
    }
}
