package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.RatingData.RatingView;
import ar.edu.uade.skillswap.application.model.RatingData.SubmitCommand;

/** Inbound port for verified ratings. */
public interface RatingUseCases {
    RatingView submit(SubmitCommand command);
}
