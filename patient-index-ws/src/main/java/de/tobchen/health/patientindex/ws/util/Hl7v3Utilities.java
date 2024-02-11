package de.tobchen.health.patientindex.ws.util;

import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000100UV01Device;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000100UV01Receiver;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000100UV01Sender;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01Device;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01Receiver;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01Sender;

public abstract class Hl7v3Utilities
{
    public static MCCIMT000300UV01Sender convertReceiverToSender300(MCCIMT000100UV01Receiver receiver)
    {
        var sender = new MCCIMT000300UV01Sender();

        sender.setDevice(convertDeviceToDevice300(receiver.getDevice()));

        sender.setTypeCode(receiver.getTypeCode());

        return sender;
    }

    public static MCCIMT000300UV01Receiver convertSenderToReceiver300(MCCIMT000100UV01Sender sender)
    {
        var receiver = new MCCIMT000300UV01Receiver();

        receiver.setDevice(convertDeviceToDevice300(sender.getDevice()));

        receiver.setTypeCode(sender.getTypeCode());

        return receiver;
    }

    public static MCCIMT000300UV01Device convertDeviceToDevice300(MCCIMT000100UV01Device source)
    {
        var target = new MCCIMT000300UV01Device();

        var targetIdList = target.getId();
        for (var id : source.getId())
        {
            targetIdList.add(id);
        }

        target.setClassCode(source.getClassCode());

        target.setDeterminerCode(source.getDeterminerCode());
        
        return target;
    }
}
