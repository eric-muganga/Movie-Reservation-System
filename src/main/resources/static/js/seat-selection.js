document.addEventListener("DOMContentLoaded", () => {
    const config = document.getElementById("seat-selection-config");

    const showtimeId = Number(config.dataset.showtimeId);
    const debugUser = config.dataset.debugUser;

    const loadingState = document.getElementById("loading-state");
    const errorState = document.getElementById("error-state");
    const errorMessage = document.getElementById("error-message");
    const retryButton = document.getElementById("retry-button");

    const content = document.getElementById("seat-selection-content");
    const auditoriumName = document.getElementById("auditorium-name");
    const summaryAuditorium = document.getElementById("summary-auditorium");

    const seatMap = document.getElementById("seat-map");
    const selectedSeatLabels = document.getElementById("selected-seat-labels");
    const selectedSeatCount = document.getElementById("selected-seat-count");
    const selectionTotal = document.getElementById("selection-total");

    const continueButton = document.getElementById("continue-button");
    const checkoutError = document.getElementById("checkout-error");

    let seatingData = null;
    let selectedSeats = new Map();

    retryButton.addEventListener("click", loadSeating);

    continueButton.addEventListener("click", async () => {
        await continueToPayment();
    });

    loadSeating();

    async function loadSeating() {
        resetPageState();

        try {
            const response = await fetch(`/api/showtimes/${showtimeId}/seating`, {
                headers: {
                    "X-Debug-User": debugUser
                }
            });

            if (!response.ok) {
                throw new Error(`Unable to load seating: ${response.status}`);
            }

            seatingData = await response.json();

            renderSeating();
            updateSelectionSummary();

            loadingState.hidden = true;
            content.hidden = false;
        } catch (error) {
            console.error("Unable to load seating", error);

            loadingState.hidden = true;
            errorMessage.textContent =
                "Unable to load seat availability. Please try again.";
            errorState.hidden = false;
        }
    }

    function resetPageState() {
        loadingState.hidden = false;
        errorState.hidden = true;
        content.hidden = true;
        checkoutError.hidden = true;
        checkoutError.textContent = "";
        continueButton.disabled = true;
    }

    function renderSeating() {
        seatMap.innerHTML = "";

        auditoriumName.textContent =
            `Auditorium: ${seatingData.auditoriumName}`;

        summaryAuditorium.textContent = seatingData.auditoriumName;

        if (!seatingData.rows || seatingData.rows.length === 0) {
            seatMap.innerHTML = `
                <div class="state-message">
                    No seats are configured for this auditorium.
                </div>
            `;
            return;
        }

        seatingData.rows.forEach(row => {
            const rowElement = document.createElement("div");
            rowElement.className = "seat-row";

            const rowLabel = document.createElement("span");
            rowLabel.className = "seat-row-label";
            rowLabel.textContent = row.rowLabel;

            const seatsElement = document.createElement("div");
            seatsElement.className = "seat-row-seats";

            row.seats.forEach((seat, index) => {
                const seatButton = createSeatButton(row.rowLabel, seat);
                seatsElement.appendChild(seatButton);

                const isNotLastSeat = index < row.seats.length - 1;
                const isEndOfSeatBlock = (index + 1) % 5 === 0;

                if (isEndOfSeatBlock && isNotLastSeat) {
                    const aisle = document.createElement("span");
                    aisle.className = "seat-aisle";
                    aisle.setAttribute("aria-hidden", "true");
                    seatsElement.appendChild(aisle);
                }
            });

            rowElement.appendChild(rowLabel);
            rowElement.appendChild(seatsElement);
            seatMap.appendChild(rowElement);
        });
    }

    function createSeatButton(rowLabel, seat) {
        const label = `${rowLabel}${seat.seatNumber}`;

        const button = document.createElement("button");
        button.type = "button";
        button.className = "seat";
        button.textContent = seat.seatNumber;
        button.dataset.seatId = seat.seatId;
        button.dataset.seatLabel = label;
        button.setAttribute("aria-label", `Seat ${label}`);

        if (seat.status === "RESERVED") {
            button.classList.add("seat-reserved");
            button.disabled = true;
            button.title = `Seat ${label} is reserved`;
            return button;
        }

        if (seat.status === "LOCKED") {
            button.classList.add("seat-locked");
            button.disabled = true;
            button.title = `Seat ${label} is temporarily unavailable`;
            return button;
        }

        if (seat.status !== "AVAILABLE") {
            button.disabled = true;
            button.title = `Seat ${label} is unavailable`;
            return button;
        }

        button.classList.add("seat-available");
        button.title = `Select seat ${label}`;

        button.addEventListener("click", () => {
            toggleSeatSelection(seat, label, button);
        });

        return button;
    }

    function toggleSeatSelection(seat, label, button) {
        if (selectedSeats.has(seat.seatId)) {
            selectedSeats.delete(seat.seatId);
            button.classList.remove("seat-selected");
            button.classList.add("seat-available");
            button.setAttribute("aria-pressed", "false");
        } else {
            selectedSeats.set(seat.seatId, {
                id: seat.seatId,
                label
            });

            button.classList.remove("seat-available");
            button.classList.add("seat-selected");
            button.setAttribute("aria-pressed", "true");
        }

        updateSelectionSummary();
    }

    function updateSelectionSummary() {
        const selected = Array.from(selectedSeats.values());
        const seatCount = selected.length;
        const total = getBasePrice() * seatCount;

        selectedSeatCount.textContent = seatCount;

        selectedSeatLabels.textContent =
            seatCount === 0
                ? "No seats selected"
                : selected.map(seat => seat.label).join(", ");

        selectionTotal.textContent = formatPrice(total);

        continueButton.disabled = seatCount === 0;
    }

    async function continueToPayment() {
        const seatIds = Array.from(selectedSeats.keys());

        if (seatIds.length === 0) {
            return;
        }

        setCheckoutLoading(true);
        clearCheckoutError();

        try {
            /*
             * startCheckout() already validates seats, creates active locks,
             * and creates the PENDING reservation inside one transaction.
             *
             * Do not call POST /api/reservations/locks here as well.
             * Calling both endpoints creates duplicate locking work and can
             * make the reservation request conflict with the user's own lock.
             */
            const reservation = await createReservation(seatIds);

            window.location.href =
                `/reservations/${reservation.reservationId}/confirmation`;
        } catch (error) {
            console.error("Unable to create reservation", error);

            showCheckoutError(
                error.message ||
                "Unable to reserve the selected seats. Please try again."
            );

            /*
             * Do not call loadSeating() here.
             * It rebuilds the seat map and removes selected seat state.
             */
        } finally {
            setCheckoutLoading(false);
        }
    }


    async function createReservation(seatIds) {
        const response = await fetch("/api/reservations", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Debug-User": debugUser
            },
            body: JSON.stringify({
                showtimeId,
                seatIds
            })
        });

        if (!response.ok) {
            throw await toApiError(
                response,
                "Unable to create reservation."
            );
        }

        return response.json();
    }

    async function toApiError(response, fallbackMessage) {
        try {
            const body = await response.json();
            return new Error(body.message || fallbackMessage);
        } catch {
            return new Error(fallbackMessage);
        }
    }

    function getBasePrice() {
        const basePrice = Number(seatingData?.basePrice);

        return Number.isFinite(basePrice) ? basePrice : 0;
    }

    function formatPrice(value) {
        return new Intl.NumberFormat("en", {
            style: "currency",
            currency: "EUR"
        }).format(value);
    }

    function setCheckoutLoading(isLoading) {
        continueButton.disabled = isLoading || selectedSeats.size === 0;
        continueButton.textContent = isLoading
            ? "Reserving seats..."
            : "Continue to payment";
    }

    function clearCheckoutError() {
        checkoutError.hidden = true;
        checkoutError.textContent = "";
    }

    function showCheckoutError(message) {
        checkoutError.textContent = message;
        checkoutError.hidden = false;
    }
});