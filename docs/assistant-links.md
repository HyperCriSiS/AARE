# Car links

Assistant apps such as Claude or ChatGPT can't call AARE's car tools: they only write text, and
they have no tool to open a link on the phone. Car links close that gap without a touch from the
driver:

1. The assistant answers as usual and puts a car link behind a single emoji.
2. The voice reading the reply aloud skips the emoji and the address behind it.
3. A MacroDroid macro sees the emoji on the screen and taps it.
4. Android hands the link to AARE instead of the browser, and AARE runs the car tool.

## Links

`https://hypercrisis.github.io/AARE/go/<tool>?<arguments>`

| Link | Does |
|---|---|
| `…/go/car_navigate?destination=Berlin` | navigates to the place in the car's navigation app |
| `…/go/car_media?action=next` | `play`, `pause`, `next` or `previous` |
| `…/go/car_display?mode=dark` | `dark`, `light` or `auto` (the car's day/night setting) |

AARE runs a link only when:

- the car (or the car screen preview) is connected,
- the app that opened it is the voice assistant chosen in AARE (*Voice assistant* page),
- the tool is one of the three above. Opening apps, reading messages and replying aren't
  reachable by link: anything sent on the driver's behalf needs the assistant's confirmation.

The links need no setting in Android: when AARE starts a hosted app through Shizuku or root, it
approves its own link domain, so Android opens them in AARE without asking.

## Assistant instructions

Put this into the assistant's instructions, for example a Claude project used in the car or
ChatGPT's custom instructions:

```
You are my assistant in the car. Answer in one or two short sentences.
When I want to drive somewhere, play, pause, skip or go back in music, or switch the
car screen to dark or light, answer briefly and add a last line with only an emoji
that links to the car action. Only one such line per answer.

- Navigate:   [🧭](https://hypercrisis.github.io/AARE/go/car_navigate?destination=<place, URL-encoded>)
- Play:       [▶️](https://hypercrisis.github.io/AARE/go/car_media?action=play)
- Pause:      [⏸️](https://hypercrisis.github.io/AARE/go/car_media?action=pause)
- Next:       [⏭️](https://hypercrisis.github.io/AARE/go/car_media?action=next)
- Previous:   [⏮️](https://hypercrisis.github.io/AARE/go/car_media?action=previous)
- Dark/light: [🌙](https://hypercrisis.github.io/AARE/go/car_display?mode=dark) or
              [☀️](https://hypercrisis.github.io/AARE/go/car_display?mode=light)

Example: "Take me to Berlin main station" →
Alright, have a good trip!
[🧭](https://hypercrisis.github.io/AARE/go/car_navigate?destination=Berlin%20Hauptbahnhof)

If the place is unclear, ask first and add no link.
```

## MacroDroid macro

- **Trigger:** *Screen Content*, only in the assistant app (for example Claude), text contains
  `🧭`. Add one trigger per emoji in use.
- **Action:** *UI Interaction → Click* on the same text (the emoji).
- **Constraint (optional):** only while the phone is connected to the car.

MacroDroid needs its accessibility service for both. The tap comes from inside the assistant app,
so Android reports the assistant as the link's sender, which AARE requires.

## Not tested yet

- A tap on a link in the Claude app's voice mode in the car, and whether it stops voice mode.
- Whether Android reports the assistant as the sender for every assistant app.
- Whether MacroDroid sees the assistant when AARE shows it on the car screen's virtual display.
- Music search and phone calls have no car link; they still need the phone's own apps.
