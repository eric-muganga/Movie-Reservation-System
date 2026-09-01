document.addEventListener("DOMContentLoaded", async () => {
    const config = document.getElementById("customer-movies-config");
    const selectedDate = config.dataset.selectedDate;

    const loadingState = document.getElementById("loading-state");
    const errorState = document.getElementById("error-state");
    const emptyState = document.getElementById("empty-state");
    const movieGrid = document.getElementById("movie-grid");
    const dateLabel = document.getElementById("selected-date-label");

    const movieTemplate = document.getElementById("movie-card-template");
    const showtimeTemplate = document.getElementById("showtime-template");

    dateLabel.textContent = `Showtimes for ${formatDate(selectedDate)}`;

    try {
        const response = await fetch(
            `/api/movies?date=${encodeURIComponent(selectedDate)}`
        );

        if (!response.ok) {
            throw new Error(`Movie API returned ${response.status}`);
        }

        const movies = await response.json();

        loadingState.hidden = true;

        if (!movies || movies.length === 0) {
            emptyState.hidden = false;
            return;
        }

        movies.forEach(movie => {
            const card = movieTemplate.content.cloneNode(true);

            const title = card.querySelector(".movie-title");
            const duration = card.querySelector(".movie-duration");
            const description = card.querySelector(".movie-description");
            const poster = card.querySelector(".movie-poster");
            const fallback = card.querySelector(".poster-fallback");
            const genreList = card.querySelector(".genre-list");
            const showtimeList = card.querySelector(".showtime-list");

            title.textContent = movie.title;
            duration.textContent = `${movie.durationMinutes} min`;
            description.textContent = movie.description || "No description available.";

            if (movie.posterUrl) {
                poster.src = movie.posterUrl;
                poster.alt = `${movie.title} poster`;

                poster.addEventListener("error", () => {
                    poster.hidden = true;
                    fallback.hidden = false;
                });
            } else {
                poster.hidden = true;
                fallback.hidden = false;
            }

            (movie.genres || []).forEach(genre => {
                const badge = document.createElement("span");
                badge.className = "genre-badge";
                badge.textContent = genre;
                genreList.appendChild(badge);
            });

            const showtimes = movie.showtimes || [];

            if (showtimes.length === 0) {
                const noShowtimes = document.createElement("p");
                noShowtimes.className = "no-showtimes";
                noShowtimes.textContent = "No showtimes available.";
                showtimeList.appendChild(noShowtimes);
            }

            showtimes.forEach(showtime => {
                const showtimeElement =
                    showtimeTemplate.content.cloneNode(true);

                const link = showtimeElement.querySelector(".showtime-link");
                const time = showtimeElement.querySelector(".showtime-time");
                const auditorium =
                    showtimeElement.querySelector(".showtime-auditorium");
                const price = showtimeElement.querySelector(".showtime-price");

                link.href = `/showtimes/${showtime.id}/seats`;

                time.textContent = formatTime(showtime.startTime);
                auditorium.textContent = showtime.auditoriumName;
                price.textContent = formatPrice(showtime.basePrice);

                showtimeList.appendChild(showtimeElement);
            });

            movieGrid.appendChild(card);
        });

        movieGrid.hidden = false;
    } catch (error) {
        console.error("Unable to load movies", error);

        loadingState.hidden = true;
        errorState.hidden = false;
    }

    function formatDate(value) {
        return new Intl.DateTimeFormat("en", {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric"
        }).format(new Date(`${value}T00:00:00`));
    }

    function formatTime(value) {
        return new Intl.DateTimeFormat("en", {
            hour: "2-digit",
            minute: "2-digit",
            hour12: false
        }).format(new Date(value));
    }

    function formatPrice(value) {
        return new Intl.NumberFormat("en", {
            style: "currency",
            currency: "EUR"
        }).format(value);
    }
});