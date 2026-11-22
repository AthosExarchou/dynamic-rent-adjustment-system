# Image Credits and Licensing

This document records the third-party visual assets and icon libraries used by the Dynamic Rent Adjustment System (DRAS), including their sources, applicable licenses, and intended use.

---

## 1. Local Deployment Demo Images (Pexels)

| Field | Details |
|---|---|
| **Asset Description** | 10 high-resolution architectural and apartment interior/exterior photos |
| **Provider** | [Pexels](https://www.pexels.com) |
| **Files** | `image1.jpg` through `image10.jpg` |
| **Local Deployment Paths** | `frontend/public/images/demo/`, `uploads/images/demo/` |
| **Served Routes** | `/images/demo/image1.jpg` - `/images/demo/image10.jpg` (frontend), `/api/images/demo/image1.jpg` - `/api/images/demo/image10.jpg` (backend) |
| **License** | [Pexels License](https://www.pexels.com/license/) |
| **Permitted Use** | Free to use for personal and commercial projects without prior permission |
| **Modifications** | Permitted under the Pexels License |
| **Attribution** | Not required; documented herein for project transparency |

**Intended Use**

- **Active placeholder:** `image1.jpg` is used in every environment when a listing has no displayable image. If that file fails to load, `image2.jpg` is used as a local secondary fallback. External scraped photographs are not shown in the production UI.
- **Local & Offline Deployments:** Used to populate sample listings and demonstration environments so local instances run completely offline without external CDN dependencies.
- **Generic Architectural Photography:** Illustrative demo photography used for UI testing; does not depict specific properties in the Athens dataset.

---

## 2. Lucide Icons

| Field | Details |
|---|---|
| **Library** | [Lucide](https://lucide.dev) |
| **Source Repository** | [lucide-icons/lucide](https://github.com/lucide-icons/lucide) |
| **License** | [ISC License](https://github.com/lucide-icons/lucide/blob/main/LICENSE) |
| **Usage** | Interface icons for navigation, actions, property attributes, and status indicators |

Lucide icons are used in accordance with the applicable license. The license and copyright notices should be retained where required, particularly when distributing copies of the licensed software or assets.

---

## Notes

- Third-party assets remain subject to their respective license terms.
- This document records asset sources and intended use; it does not imply ownership of third-party material.
- Asset licensing should be reviewed if the application's use or distribution changes.
