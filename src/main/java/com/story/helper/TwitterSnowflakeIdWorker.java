package com.story.helper;

/**
 * twitter在把存储系统从MySQL迁移到Cassandra的过程中由于Cassandra没有顺序ID生成机制，于是自己开发了一套全局唯一ID生成服务：Snowflake。
 * 1 41位的时间序列（精确到毫秒，41位的长度可以使用69年）
 * 2 10位的机器标识（10位的长度最多支持部署1024个节点）
 * 3 12位的计数顺序号（12位的计数顺序号支持每个节点每毫秒产生4096个ID序号） 最高位是符号位，始终为0。
 * 优点：高性能，低延迟；独立的应用；按时间有序。 缺点：需要独立的开发和部署。
 *
 * @author storys.zhang@gmail.com
 * <p>
 * Created at 2019/4/4 by Storys.Zhang
 */
public class TwitterSnowflakeIdWorker {

    private final long workerId;
    private final long twepoch = 1288834974657L;
    private long sequence = 0L;
    private final long workerIdBits = 4L;
    public final long maxWorkerId = -1L ^ -1L << workerIdBits;
    private final long sequenceBits = 10L;
    private final long workerIdShift = sequenceBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits;
    public final long sequenceMask = -1L ^ -1L << sequenceBits;
    private long lastTimestamp = -1L;

    public TwitterSnowflakeIdWorker(final long workerId) {
        super();
        if (workerId > this.maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format(
                    "worker Id can't be greater than %d or less than 0",
                    this.maxWorkerId));
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = this.timeGen();
        if (this.lastTimestamp == timestamp) {
            this.sequence = (this.sequence + 1) & this.sequenceMask;
            if (this.sequence == 0) {
                System.out.println("###########" + sequenceMask);
                timestamp = this.tilNextMillis(this.lastTimestamp);
            }
        } else {
            this.sequence = 0;
        }
        if (timestamp < this.lastTimestamp) {
            try {
                throw new Exception(
                        String.format(
                                "Clock moved backwards. Refusing to generate id for %d milliseconds",
                                this.lastTimestamp - timestamp));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.lastTimestamp = timestamp;
        long nextId = ((timestamp - twepoch << timestampLeftShift))
                | (this.workerId << this.workerIdShift) | (this.sequence);
/*        System.out.println("timestamp:" + timestamp + ",timestampLeftShift:"
                + timestampLeftShift + ",nextId:" + nextId + ",workerId:"
                + workerId + ",sequence:" + sequence);*/
        return nextId;
    }

    private long tilNextMillis(final long lastTimestamp) {
        long timestamp = this.timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = this.timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }


    public static void main(String[] args) {

        TwitterSnowflakeIdWorker worker2 = new TwitterSnowflakeIdWorker(3);
        System.out.println(worker2.maxWorkerId);
        for (int i = 0; i < 10; i++) {
            System.out.println(worker2.nextId());
        }
    }
}
