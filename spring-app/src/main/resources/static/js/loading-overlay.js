(() => {
  const overlay = document.getElementById("loading-overlay");
  const message = document.getElementById("loading-message");
  if (!overlay || !message) return;

  const hide = () => {
    overlay.classList.remove("is-visible");
    overlay.setAttribute("aria-hidden", "true");
    document.body.classList.remove("is-loading");
  };

  document.querySelectorAll("form[data-loading-message]").forEach((form) => {
    form.addEventListener("submit", (event) => {
      if (event.submitter?.hasAttribute("data-skip-loading")) return;

      event.preventDefault();
      message.textContent = form.dataset.loadingMessage;
      overlay.classList.add("is-visible");
      overlay.setAttribute("aria-hidden", "false");
      document.body.classList.add("is-loading");

      const submitButton = form.querySelector('[type="submit"]');
      if (submitButton) submitButton.disabled = true;

      // Dwie klatki dają przeglądarce czas na narysowanie nakładki przed nawigacją.
      requestAnimationFrame(() => requestAnimationFrame(() => {
        HTMLFormElement.prototype.submit.call(form);
      }));
    });
  });

  window.addEventListener("pageshow", hide);
})();
