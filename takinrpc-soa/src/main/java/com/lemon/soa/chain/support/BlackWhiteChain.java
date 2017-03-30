package com.lemon.soa.chain.support;

import java.util.Map;

import com.lemon.soa.chain.AbstractNeuralChain;
import com.lemon.soa.common.exception.AlarmException;
import com.lemon.soa.common.exception.ProcessorException;
import com.lemon.soa.common.spi.SPI;
import com.lemon.soa.moduler.ModulerType;
import com.lemon.soa.moduler.extension.blackwhite.IBlackWhiteProcessor;
import com.lemon.soa.moduler.extension.echosound.EchoSoundType;
import com.lemon.soa.moduler.senior.alarm.IAlarmType;
import com.lemon.soa.processor.INeuralProcessor;

/**
 * 黑白名单调用链
 * 
 * me
 *
 * @param <REQ>
 * @param <RES>
 */
@SPI(order = 2)
public class BlackWhiteChain<REQ, RES> extends AbstractNeuralChain<REQ, RES> {

    @Override
    public RES chain(REQ req, final String neuralId, final EchoSoundType echoSoundType, final Map<String, Object> blackWhiteIdKeyVals, final INeuralProcessor<REQ, RES> processor, Object... args) {

        // $NON-NLS-黑白名单开始$
        return moduler.getBlackWhite().blackwhite(req, blackWhiteIdKeyVals, new IBlackWhiteProcessor<REQ, RES>() {

            @Override
            public RES processor(REQ req, Object... args) throws ProcessorException {
                return neuralChain.chain(req, neuralId, echoSoundType, blackWhiteIdKeyVals, processor, args);
            }

            /**
             * 告警
             */
            @Override
            public void alarm(IAlarmType alarmType, REQ req, RES res, Throwable t, Object... args) throws AlarmException {
                processor.alarm(ModulerType.BlackWhite, alarmType, req, res, t, args);
            }

        });

    }

}