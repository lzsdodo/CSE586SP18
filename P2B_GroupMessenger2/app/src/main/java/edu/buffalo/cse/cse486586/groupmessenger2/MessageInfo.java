package edu.buffalo.cse.cse486586.groupmessenger2;


/*
 * Reference:
 * - Android Dev Docs:
 *      Collections: https://docs.oracle.com/javase/9/docs/api/java/util/Collections.html
 *      Comparable: https://developer.android.com/reference/java/lang/Comparable.html
 */

public class MessageInfo implements Comparable<MessageInfo>{

    // Structure: <msg, msgID, senderPID, agrProir, propPID, dilverable>
    private Integer senderID;
    private Integer msgID;
    private String msgContent;
    private Integer propPID;
    private Integer propSeqPrior;
    private Integer isDeliverable;

    public MessageInfo (Message message) {
        this.senderID = message.getSenderPID();
        this.msgID = message.getMsgID();
        this.msgContent = message.getMsgContent();
        this.propPID = message.getPropPID();
        this.propSeqPrior = message.getPropSeqPrior();
        this.isDeliverable = message.getIsDeliverable();
    }

    // setter and getter
    public Integer getPropSeqPrior () {return propSeqPrior;}
    public void setPropSeqPrior (Integer propSeqPrior) {this.propSeqPrior = propSeqPrior;}
    public void setPropPID (Integer propPID) {this.propPID = propPID;}
    public void setDiliverable (Integer diliverable) {this.isDeliverable = diliverable;}

    public Integer getMsgID () {return this.msgID;}
    public String getMsgContent () {return this.msgContent;}
    public Integer getSenderID () {return this.senderID;}
    public Integer getPropPID () {return this.propPID;}
    public Integer getIsDeliverable () {return this.isDeliverable;}

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
            if(this.isDeliverable < another.getIsDeliverable()) {
                return -1;
            } else if (this.isDeliverable == another.getIsDeliverable()) {
                if (this.msgID < another.getMsgID()) {
                    return  -1;
                }
            }
        }
        return 0;
    }


}
