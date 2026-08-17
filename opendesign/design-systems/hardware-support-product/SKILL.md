---
name: hardware-support-product-system
description: Product design system for the Customservice hardware support, diagnosis, handoff and knowledge operations surfaces.
---

# Hardware Support Product System

Use this system for terminal support, operator handoff, diagnosis flow and knowledge operations work in this repository.

## Visual premise

Treat every screen as a maintained service record, not an AI dashboard. Favor clear rulings, compact facts, timestamps, revision identifiers and editorial hierarchy. Use surfaces sparingly; information should often sit directly on the page separated by lines.

## Required semantics

- Deep green ink: product structure, navigation and primary action.
- Teal: automated diagnosis state only, never generic decoration.
- Orange: named human ownership, contact and promises.
- Red: safety stop and physical risk only.

## Typography

- Editorial heading: `Iowan Old Style`, `Songti SC`, serif.
- Product body: `Avenir Next`, `Manrope`, `PingFang SC`, sans-serif.
- Identifiers and timestamps: `JetBrains Mono`, `SFMono-Regular`, monospace.

## Layout rules

- Avoid hero banners, equal-width feature cards and decorative KPI grids.
- Prefer one dominant work surface with a narrow annotation or evidence rail.
- Use 1px rules, square-ish corners and small labels instead of shadows.
- Allow asymmetry. Do not distribute every region evenly.
- In terminal support, the phone screen is the artifact; supporting explanation stays outside it.

## Interaction rules

- Every visible control responds.
- One primary action at a time.
- Safety stop removes normal continuation actions.
- Human handoff exposes owner, SLA, next contact and preserved evidence.
- Motion is limited to screen change and current-step emphasis.

Read `tokens/colors_and_type.css`, `brand/voice-and-tone.md`, and `brand/style-notes.md` before building.
