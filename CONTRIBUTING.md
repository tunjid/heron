# Contributing to Heron

Thanks for wanting to contribute to Heron! This doc collects a few things I care about as the
maintainer. Most of them personal, some of them practical.

The [README](README.md) is the general overview of the app's architecture build/publishing, and
other general info. [AGENTS.md](AGENTS.md) is the terse map of where things live, and as you may
expect general info useful for LLMs like the pattern to copy, and commands to run. 

This doc is more for the humans and the coding agents directed by them.

## In the spirit of the codebase

Please try to make changes in the spirit of the existing codebase. Please do not introduce one-off
patterns just to support your feature. Heron is a medium-sized app, but it's quite consistent in the
way it works, and I'd like to keep it that way.

## Splitting pull requests

If changes have data layer and UI layer changes, the changes should be separate PRs, with the data
layer first. This makes it easier to see how the changes fit in the larger context before UI
specifics fit in.

## Reference PRs for issues

Reference PRs made with LLMs are welcome, as long as there's no expectation to actually merge them.
They can be useful for highlighting a bug or a behavior you'd like, and I reserve the right to edit
them to fit the app's architecture as I see fit.

## Working with LLMs

If changes are made with LLMs, tell-tale LLM output like verbose comments should be trimmed or
deleted. Comments can be useful, and Heron has a few where it matters, but too many reduce their
utility as it just adds noise.

In using LLMs, please make sure they aren't listed as authors in the commit. Anthropomorphizing
LLMs is a little uncanny; I'd much rather just see you, the contributor, owning what you committed
rather than advertising an agent you directed. Also, please take care to edit or remove obvious LLM
artifacts. Verbose comments especially, heron uses comments sparingly, so they add value when
encountered. Spurious comments are just noise. I also rarely use the em-dash, so please keep the
voice in comments consistent. I'd much rather a semicolon if you need that sort of break.

## Personal taste

Taste is a rather subjective thing. I spend a lot of time making design decisions about what
goes where, and what UI elements look like what. I try to accommodate some customization requests,
however Heron is not primarily about customization. It's an expression of my taste and it is
wholly subjective. I reserve the right to deny any design changes without needing to give a
reason.

Please use string resources where you can.

## Have fun

Try to have fun. Heron is a passion project for me, and where I go as my coding happy place outside
of work. I have certain quirks in it, like my code style and other things, which are definitely more
stylistic and personal, and that is very much intended. Please try to respect that.
