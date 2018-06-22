package fr.husta.test;

import fr.husta.test.ansi.AnsiColor;
import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * See doc : https://doc.ubuntu-fr.org/smartcards#javaxsmartcardio
 */
public class SmartCardIOTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartCardIOTest.class);

    @Test
    public void showListTerminals() throws CardException {
        // Show the list of available terminals
        // On Windows see HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Cryptography\Calais\Readers
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals cardTerminals = factory.terminals();
        List terminals = null;
        try {
            terminals = cardTerminals.list();
        } catch (CardException e) {
            Throwable cause = e.getCause();
            // filter sun.security.smartcardio.PCSCException exception
            if (cause != null &&
                    "sun.security.smartcardio.PCSCException".equals(cause.getClass().getName())) {
                LOGGER.debug("Exception message : {}", e.getMessage());
                String causeMessage = cause.getMessage();
                LOGGER.debug("Exception's cause message : {}", causeMessage);
                Assert.fail("Can't test : " + causeMessage);
            }
            throw e;
        }

        System.out.println("Terminals count: " + terminals.size());
        System.out.println(AnsiColor.bold("Terminals: "));
        for (Object terminal : terminals) {
            System.out.println(" * " + terminal);
        }

        if (!terminals.isEmpty()) {
            // Get the first terminal in the list
            CardTerminal terminal = (CardTerminal) terminals.get(0);

            if (terminal.isCardPresent()) {
                // Establish a connection with the card using
                // "T=0", "T=1", "T=CL" or "*"
                Card card = terminal.connect("*");
                System.out.println(AnsiColor.bold("Card: (Terminal #1)"));
                System.out.println(" * " + card);

                // Get ATR (Answer-To-Reset)
                ATR cardATR = card.getATR();
                byte[] baATR = cardATR.getBytes();
                // See there if ATR is known: http://ludovic.rousseau.free.fr/softwares/pcsc-tools/smartcard_list.txt
                System.out.println("ATR: " + toHexaString(baATR, ":"));

                // Select Card Manager
                // - Establish channel to exchange APDU
                // - Send SELECT Command APDU
                // - Show Response APDU
                CardChannel channel = card.getBasicChannel();

                //SELECT Command
                // See GlobalPlatform Card Specification (e.g. 2.2, section 11.9)
                // CLA: 00
                // INS: A4
                // P1: 04 i.e. b3 is set to 1, means select by name
                // P2: 00 i.e. first or only occurence
                // Lc: 08 i.e. length of AID see below
                // Data: A0 00 00 00 03 00 00 00
                // AID of the card manager,
                // in the future should change to A0 00 00 01 51 00 00

                byte[] baCommandAPDU = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x08, (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                System.out.println("APDU Command  >>> " + toHexaString(baCommandAPDU));

                ResponseAPDU r = channel.transmit(new CommandAPDU(baCommandAPDU));
                System.out.println("APDU Response <<< " + toHexaString(r.getBytes()));

                boolean reset = false;
                card.disconnect(reset);
            }
        }
    }

    public static String toHexaString(byte[] bytes) {
        return Hex.encodeHexString(bytes, false);
    }

    public static String toHexaString(byte[] bytes, String separator) {
        if (separator == null) {
            separator = " ";
        }
        String res = Hex.encodeHexString(bytes, false);
        int nbBytes = res.length() / 2;
        StringBuilder finalString = new StringBuilder();
        for (int i = 0; i < nbBytes; i++) {
            finalString.append(res, i * 2, i * 2 + 2);
            if (i < nbBytes - 1) {
                finalString.append(separator);
            }
        }
        return finalString.toString();
    }

    @Test
    public void testToHexaString_withSeparator() {
        byte[] input = {(byte) 0xAB, (byte) 0xCD, (byte) 0xEF}; // AB CD EF
        String hexaString = toHexaString(input, ":");
        assertEquals("AB:CD:EF", hexaString);
    }

    public static String toBinaryString(byte[] bytes) {
        return BinaryCodec.toAsciiString(bytes);
    }

}
