package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.BookingException;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CinemaTicketsServiceImpl.class);

  private static final int MAX_TOTAL_TICKET_COUNT = 25;
  private static final BigDecimal ADULT_PRICE = new BigDecimal("25.99");
  private static final BigDecimal CHILD_PRICE = new BigDecimal("17.50");
  private static final BigDecimal INFANT_PRICE = BigDecimal.ZERO;

  private final PaymentService paymentService;
  private final SeatReservationService seatReservationService;

  public CinemaTicketsServiceImpl(
      PaymentService paymentService, SeatReservationService seatReservationService) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
  }

  @Override
  public BookingConfirmation purchaseTickets(
      final Long accountId, final TicketRequest... ticketRequests) throws InvalidBookingException {
    validatePurchaseTickets(accountId, ticketRequests);

    try {
      var paymentAmount = calculatePaymentAmount(ticketRequests);
      paymentService.debitAccount(accountId, paymentAmount);

      var seatCount = calculateSeatCount(ticketRequests);
      seatReservationService.reserveSeats(accountId, seatCount);

      return new BookingConfirmation(accountId, seatCount, paymentAmount);
    } catch (Exception e) {
      LOGGER.error("exception in purchaseTickets for accountId {}", accountId, e);
      throw new BookingException("exception in purchaseTickets for accountId " + accountId, e);
    }
  }

  private void validatePurchaseTickets(final Long accountId, final TicketRequest... ticketRequests)
      throws InvalidBookingException {
    if (accountId == null) {
      throw new InvalidBookingException("accountId must not be null");
    }

    if (accountId <= 0) {
      throw new InvalidBookingException("accountId must be greater than zero");
    }

    if (ticketRequests == null) {
      throw new InvalidBookingException("ticketRequests must not be null");
    }

    if (ticketRequests.length == 0) {
      throw new InvalidBookingException("ticketRequests must not be empty");
    }

    var totalTicketCount = 0L;
    var adultTicketCount = 0L;
    var infantTicketCount = 0L;
    for (var ticketRequest : ticketRequests) {
      validateTicketRequest(ticketRequest);

      totalTicketCount += ticketRequest.ticketCount();
      switch (ticketRequest.type()) {
        case ADULT -> adultTicketCount += ticketRequest.ticketCount();
        case INFANT -> infantTicketCount += ticketRequest.ticketCount();
        case CHILD -> {
          // child ticketCount is not required for validation logic
        }
      }
    }

    if (adultTicketCount <= 0) {
      throw new InvalidBookingException("adult ticketCount must be greater than zero");
    }

    if (totalTicketCount > MAX_TOTAL_TICKET_COUNT) {
      throw new InvalidBookingException(
          "total ticketCount must not exceed " + MAX_TOTAL_TICKET_COUNT);
    }

    if (infantTicketCount > adultTicketCount) {
      throw new InvalidBookingException("infant ticketCount must not exceed adult ticketCount");
    }
  }

  private void validateTicketRequest(final TicketRequest ticketRequest) {
    if (ticketRequest == null) {
      throw new InvalidBookingException("ticketRequest must not be null");
    }

    if (ticketRequest.type() == null) {
      throw new InvalidBookingException("ticketRequest type must not be null");
    }

    if (ticketRequest.ticketCount() <= 0) {
      throw new InvalidBookingException("ticketRequest ticketCount must be greater than zero");
    }
  }

  private BigDecimal calculatePaymentAmount(final TicketRequest... ticketRequests) {
    var totalPaymentAmount = BigDecimal.ZERO;
    for (var ticketRequest : ticketRequests) {
      var count = BigDecimal.valueOf(ticketRequest.ticketCount());
      var ticketRequestPaymentAmount = priceOf(ticketRequest.type()).multiply(count);
      totalPaymentAmount = totalPaymentAmount.add(ticketRequestPaymentAmount);
    }

    return totalPaymentAmount;
  }

  private BigDecimal priceOf(final TicketType ticketType) {
    return switch (ticketType) {
      case ADULT -> ADULT_PRICE;
      case CHILD -> CHILD_PRICE;
      case INFANT -> INFANT_PRICE;
    };
  }

  private Long calculateSeatCount(final TicketRequest... ticketRequests) {
    return Arrays.stream(ticketRequests)
        .filter(t -> t.type() != TicketType.INFANT)
        .mapToLong(TicketRequest::ticketCount)
        .sum();
  }
}
