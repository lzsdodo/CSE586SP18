package edu.buffalo.cse.cse486586.groupmessenger2;


/*
 * Reference:
 * - Android Dev Docs:
 *      Collections: https://docs.oracle.com/javase/9/docs/api/java/util/Collections.html
 *      Comparable: https://developer.android.com/reference/java/lang/Comparable.html
 */

public class MessageInfo implements Comparable<MessageInfo>{

    // Structure: <msg, msgID, senderPID, agrProir, propPID, dilverable>
    private int senderID;
    private int msgID;
    private String msgContent;
    private int propPID;
    private int propSeqPrior;
    private int isDeliverable; // 1: diliverable; 0: undeliverable

    public MessageInfo (Message message) {
        this.senderID = message.getSenderPID();
        this.msgID = message.getMsgID();
        this.msgContent = message.getMsgContent();
        this.propPID = message.getPropPID();
        this.propSeqPrior = message.getPropSeqPrior();
        this.isDeliverable = 0;
    }

    // setter and getter
    public int getMsgID () {return this.msgID;}
    public String getMsgContent () {return this.msgContent;}
    public int getPropSeqPrior () {return propSeqPrior;}
    public int getDeliverable () {return this.isDeliverable;}

    public void setPropPID (Integer propPID) {this.propPID = propPID;}
    public void setPropSeqPrior (Integer propSeqPrior) {this.propSeqPrior = propSeqPrior;}
    public void setDiliverable (Integer diliverable) {this.isDeliverable = diliverable;}

    @Override
    public String toString () {
        return "MsgeInfo:\n" +
                "\tsenderID: " + senderID + "; msgID=" + msgID + "\n" +
                "\tmsgContent: " + msgContent + "\n" +
                "\tPropPID-SeqPrior: " + propPID + "-" + propSeqPrior + "\n" +
                "\tIsDeliverable: " + isDeliverable;
    }

    @Override
    public int compareTo (MessageInfo another) {
        if(this.propSeqPrior < another.getPropSeqPrior()) {
            return -1;
        } else if(this.propSeqPrior == another.getPropSeqPrior()) {
            if(this.isDeliverable < another.getDeliverable()) {
                return -1;
            } else if (this.isDeliverable == another.getDeliverable()) {
                if (this.msgID < another.getMsgID()) {
                    return  -1;
                }
            }
        }
        return 0;
    }

}
