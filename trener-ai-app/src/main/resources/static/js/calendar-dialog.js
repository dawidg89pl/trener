(() => {
  document.addEventListener("click", (event) => {
    const openButton = event.target.closest("[data-dialog-target]");
    if (openButton) {
      const dialog = document.getElementById(openButton.dataset.dialogTarget);
      if (dialog && !dialog.open) dialog.showModal();
      return;
    }

    const closeButton = event.target.closest("[data-dialog-close]");
    if (closeButton) closeButton.closest("dialog")?.close();
  });

  document.querySelectorAll(".workout-dialog").forEach((dialog) => {
    dialog.addEventListener("click", (event) => {
      if (event.target === dialog) dialog.close();
    });
  });
})();
