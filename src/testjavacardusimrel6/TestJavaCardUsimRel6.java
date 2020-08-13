package testjavacardusimrel6;

// Imported packages
// specific import for Javacard API access
import javacard.framework.*; // specific import for GlobalPlatform API access
import org.globalplatform.*;

public class TestJavaCardUsimRel6 extends javacard.framework.Applet {

	private byte persoFlag;
	private static final byte ALL_DATA_PERSONALIZED = (byte) 0x03;

	// Constants not provided by the GlobalPlatform API.
	private static final byte APPLICATION_PERSONALIZED = (byte) 0x0F;
	private SecureChannel secureChannel;

	/**
	 * Default constructor
	 * Only this class's install method should create the applet object.
	 *  @param baBuffer the array constaining installation parameters
	 *  @param sOffset the starting offset in baBuffer
	 *  @param bLength the length in bytes of the data parameter in baBuffer
	 */
	protected TestJavaCardUsimRel6(byte[] baBuffer, short sOffset, byte bLength) {
		register(baBuffer, (short) (sOffset + 1), (byte) baBuffer[sOffset]);
	}

	/**
	 * Method installing the applet.
	 *  @param baBuffer the array constaining installation parameters
	 *  @param sOffset the starting offset in baBuffer
	 *  @param bLength the length in bytes of the data parameter in baBuffer
	 */
	public static void install(byte[] baBuffer, short sOffset, byte bLength) {
		// applet  instance creation 
		new TestJavaCardUsimRel6(baBuffer, sOffset, bLength);
	}

	/**
	 * Select method returns true if applet selection is supported.
	 *  @return boolean Should always be true.
	 */
	public boolean select() {
		/**@todo : PUT YOUR SELECTION ACTION HERE*/
		secureChannel = GPSystem.getSecureChannel();
		// return status of selection
		return true;
	}

	/**
	 * Deselect method called by the system in the deselection process.
	 */
	public void deselect() {
		/**@todo : PUT YOUR SELECTION ACTION HERE*/
		secureChannel.resetSecurity();
	}

	/**
	 * Method processing an incoming APDU.
	 *  @see APDU
	 *  @param apdu the incoming APDU
	 *  @exception ISOException with the response bytes defined by ISO 7816-4
	 */
	public void process(APDU apdu) {

		// get the APDU buffer
		byte[] apduBuffer = apdu.getBuffer();
		byte cla = (byte) (apduBuffer[ISO7816.OFFSET_CLA] & 0x78);

		// Check that the class byte does not have bits 7, 6, 5 or 4 set.
		if (cla != 0x00) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		// If Life Cycle State is SELECTABLE it is only possible to process personalization
		// commands.
		if (GPSystem.getCardContentState() == GPSystem.APPLICATION_SELECTABLE) {
			// Life Cycle State of the Application is SELECTABLE. The commands
			//  expected are the INITIALIZE UPDATE, EXTERNAL AUTHENTICATE and STORE DATA
			//  commands.
			short outLength = (short) 0;
			//First send the command to the Application's associated Security Domain.
			try {
				outLength = secureChannel.processSecurity(apdu);
			} catch (ISOException e) {
				if (e.getReason() == ISO7816.SW_INS_NOT_SUPPORTED
						|| e.getReason() == ISO7816.SW_CLA_NOT_SUPPORTED) {
					// command not recognized, reset the status
					// words to 0x9000 and process the command as a personalization specific command.
					e.setReason(ISO7816.SW_NO_ERROR);
				} else {
					//command recognized but could not be processed correctly.
					ISOException.throwIt(e.getReason());
				}
			}
			//check if there is any response data that has to be output.
			if (outLength != 0) {
				apdu
						.setOutgoingAndSend((short) ISO7816.OFFSET_CDATA,
								outLength);
			}
			// Once all the personalization information has been received, set the Life Cycle State
			// to PERSONALIZED.
			if (persoFlag == ALL_DATA_PERSONALIZED) {
				GPSystem.setCardContentState(APPLICATION_PERSONALIZED);
			}
			return;
		}
	}

}
