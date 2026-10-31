/*
 * The one Quick Capture box, shared by every page (see QuickCaptureRenderer). Suggests project
 * handles until a colon is typed, then the sentence verbs.
 */
(function () {
  'use strict';

  var VERBS = ['I will', 'I have committed', 'I might', 'I would like to', 'I will meet', 'I have set goal to', 'I am waiting'];

  function init() {
    var input = document.getElementById('sentenceInput');
    var box = document.getElementById('qcSuggestions');
    var namesEl = document.getElementById('qcProjectNames');
    if (!input || !box || input.getAttribute('data-qc-ready')) { return; }
    input.setAttribute('data-qc-ready', 'true');
    var projectNames = [];
    try { projectNames = JSON.parse(namesEl ? namesEl.textContent : '[]') || []; } catch (e) { projectNames = []; }

    var current = [];
    var selected = -1;

    function suggestionsFor(text) {
      var colon = text.indexOf(':');
      if (colon === -1) {
        var lower = text.toLowerCase();
        return projectNames.filter(function (name) { return name.toLowerCase().indexOf(lower) === 0; });
      }
      var before = text.substring(0, colon).trim();
      var after = text.substring(colon + 1).trim().toLowerCase();
      if (projectNames.indexOf(before) === -1) {
        var b = before.toLowerCase();
        return projectNames.filter(function (name) { return name.toLowerCase().indexOf(b) >= 0; });
      }
      if (after.length === 0) { return VERBS.slice(0); }
      return VERBS.filter(function (verb) { return verb.toLowerCase().indexOf(after) === 0 && verb.toLowerCase() !== after; });
    }

    function hide() { box.style.display = 'none'; selected = -1; }

    function render() {
      box.innerHTML = '';
      box.style.display = current.length ? 'block' : 'none';
      current.forEach(function (suggestion, i) {
        var div = document.createElement('div');
        div.className = 'qc-suggestion' + (i === selected ? ' qc-suggestion-active' : '');
        div.textContent = suggestion;
        div.addEventListener('mousedown', function (e) { e.preventDefault(); accept(suggestion); });
        box.appendChild(div);
      });
    }

    function accept(suggestion) {
      var text = input.value || '';
      if (text.indexOf(':') === -1) {
        input.value = suggestion + ': ';
      } else {
        input.value = text.split(':')[0].trim() + ': ' + suggestion + ' ';
      }
      hide();
      input.focus();
    }

    input.addEventListener('input', function () {
      var text = input.value || '';
      current = text.trim().length === 0 ? [] : suggestionsFor(text);
      selected = -1;
      render();
    });
    input.addEventListener('keydown', function (e) {
      var visible = box.style.display === 'block' && current.length > 0;
      if (!visible) { return; }
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        selected = e.key === 'ArrowDown' ? (selected + 1) % current.length : (selected - 1 + current.length) % current.length;
        render();
      } else if (e.key === 'Tab' || e.key === 'Enter') {
        e.preventDefault();
        accept(current[selected < 0 ? 0 : selected]);
      } else if (e.key === 'Escape') {
        hide();
      }
    });
    input.addEventListener('blur', function () { setTimeout(hide, 150); });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
