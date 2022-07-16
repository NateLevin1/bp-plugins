# Bridge Practice Custom Plugins
This is the main repository for all the custom plugins that BridgePractice uses.
Please write clear, meaningful commits!

### Mini-guide on how to use git
```bash
# git is command-line based so you'll need to use git bash (auto-installed on windows)
# go to the right directory
cd C:\Path\To\bp-plugins

# Now, go to your editor and make some changes!

# If you added new files you will need to run:
git add src # Make sure not to acidentally commit your pom.xml!!!

# create a new branch
git branch some-name-to-describe-the-changes-in-the-pr
# switch to that branch
git checkout my-branch

# Don't make too many changes before committing though, try to make each commit
# be specfic to one thing.
# Start your commit with the plugin you are modifying if it is a small change/bug fix
# If you are making a big change across multiple plugins you don't need to do that
git commit -m "BPBridge: Fix issue with xp not adding" # Make sure the string is descriptive!
git commit -m "BridgePracticeClub: Fix NPE" # Make sure the string is descriptive!
git commit -m "BridgePracticeLobby: Add cookie gadget" # Make sure the string is descriptive!
# send off to github
git push
# if that gives you an error message, try this first
git push --set-upstream origin main
```
