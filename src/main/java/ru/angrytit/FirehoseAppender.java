package ru.angrytit;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.kinesisfirehose.model.ResourceNotFoundException;

import java.nio.ByteBuffer;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Mikhail Tyamin <a href="mailto:mikhail.tiamine@gmail.com>mikhail.tiamine@gmail.com</a>
 */
public class FirehoseAppender extends AppenderBase<ILoggingEvent> {
    private static final String DEFAULT_ENCODING = "UTF-8";

    private PatternLayoutEncoder encoder;
    private String deliveryStream;
    private String region;
    private String encoding;
    private AmazonKinesisFirehoseClient client;

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    public String getDeliveryStream() {
        return deliveryStream;
    }

    public void setDeliveryStream(String deliveryStream) {
        this.deliveryStream = deliveryStream;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    protected void append(ILoggingEvent event) {
        String message = encoder.getLayout().doLayout(event);
        PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setDeliveryStreamName(deliveryStream);
        try {
            ByteBuffer data = ByteBuffer.wrap(message.getBytes(encoding));
            Record record = new Record().withData(data);
            putRecordRequest.setRecord(record);
            client.putRecord(putRecordRequest);
        } catch (Exception e) {
            addError("Error while communicate with Firehose", e);
        }
    }

    @Override
    public void start() {
        if (isNull(encoder)) {
            addError("No encoder set for the appender : [" + name + "]");
            return;
        }
        if (isEmpty(region)) {
            addError("No region set for the appender : [" + name + "]");
            return;
        }
        if (isEmpty(deliveryStream)) {
            addError("No deliveryStream set for the appender : [" + name + "]");
            return;
        }
        if (isEmpty(encoding)) {
            encoding = DEFAULT_ENCODING;
        }
        client = new AmazonKinesisFirehoseClient();
        try {
            client.setRegion(Region.getRegion(Regions.fromName(region)));
        } catch (IllegalArgumentException e) {
            addError("Wrong region : [" + region + "] set for the appender : [" + name + "]");
            return;
        }
        if (!isDeliveryStreamExists()) {
            return;
        }
        super.start();
    }

    private boolean isDeliveryStreamExists() {
        try {
            DescribeDeliveryStreamRequest describeDeliveryStreamRequest =
                    new DescribeDeliveryStreamRequest().withDeliveryStreamName(deliveryStream);
            client.describeDeliveryStream(describeDeliveryStreamRequest);
        } catch (ResourceNotFoundException e) {
            addError("Delivery stream : [" + deliveryStream + "] does not exist for the appender : [" + name + "]");
            return false;
        } catch (Exception e) {
            addError("Error while communicate with Firehose", e);
            return false;
        }

        return true;
    }

    @Override
    public void stop() {
        client.shutdown();
        super.stop();
    }
}
