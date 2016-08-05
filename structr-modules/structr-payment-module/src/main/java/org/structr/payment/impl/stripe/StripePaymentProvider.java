package org.structr.payment.impl.stripe;

import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.payment.api.APIError;
import org.structr.payment.api.BeginCheckoutResponse;
import org.structr.payment.api.CheckoutState;
import org.structr.payment.api.ConfirmCheckoutResponse;
import org.structr.payment.api.Payment;
import org.structr.payment.api.PaymentProvider;
import org.structr.payment.api.PaymentState;

/**
 *
 * @author Christian Morgner
 */
public class StripePaymentProvider implements PaymentProvider {

	@Override
	public BeginCheckoutResponse beginCheckout(final Payment payment, final String successUrl, final String cancelUrl) throws FrameworkException {
		throw new FrameworkException(422, "Begin checkout not supported by this payment provider. Please use the confirmCheckout endpoint.");
	}

	@Override
	public ConfirmCheckoutResponse confirmCheckout(final Payment payment, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		Stripe.apiKey = StructrApp.getConfigurationValue("stripe.apikey");

		// Create the charge on Stripe's servers - this will charge the user's card
		try {

			final Map<String, Object> chargeParams = new HashMap<>();

			chargeParams.put("amount", payment.getTotal());
			chargeParams.put("currency", payment.getCurrencyCode());
			chargeParams.put("source", token);
			chargeParams.put("description", payment.getDescription());

			Charge.create(chargeParams);

			payment.setPaymentState(PaymentState.completed);

			return new ConfirmResponse(CheckoutState.Success);

		} catch (APIException ex) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, "1", "APIException", ex.getMessage());

		} catch (APIConnectionException ex) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, "1", "APIConnectionException", ex.getMessage());

		} catch (InvalidRequestException ex) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, "1", "InvalidRequestException", ex.getMessage());

		} catch (AuthenticationException ex) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, "1", "AuthenticationException", ex.getMessage());

		} catch (CardException e) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, e.getCode(), e.getCharge(), e.getMessage());
		}
	}

	@Override
	public void cancelCheckout(final Payment payment) throws FrameworkException {

		// we only have to set the payment state
		try {

			payment.setToken(null);
			payment.setPaymentState(PaymentState.cancelled);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	private static class ConfirmResponse implements ConfirmCheckoutResponse {

		private final List<APIError> errors = new LinkedList<>();
		private CheckoutState state         = null;

		public ConfirmResponse(final CheckoutState state) {
			this(state, null, null, null);
		}

		public ConfirmResponse(final CheckoutState state, final String errorCode, final String shortMessage, final String longMessage) {

			this.state = state;

			if (errorCode != null && shortMessage != null && longMessage != null) {
				this.errors.add(new APIErrorImpl(errorCode, shortMessage, longMessage));
			}
		}

		@Override
		public CheckoutState getCheckoutState() {
			return state;
		}

		@Override
		public List<APIError> getErrors() {
			return errors;
		}
	}

	private static class APIErrorImpl implements APIError {

		private String errorCode    = null;
		private String shortMessage = null;
		private String longMessage  = null;

		public APIErrorImpl(final String errorCode, final String shortMessage, final String longMessage) {

			this.errorCode     = errorCode;
			this.shortMessage  = shortMessage;
			this.longMessage   = longMessage;
		}

		@Override
		public String getShortMessage() {
			return shortMessage;
		}

		@Override
		public String getLongMessage() {
			return longMessage;
		}

		@Override
		public String getErrorCode() {
			return errorCode;
		}
	}
}
