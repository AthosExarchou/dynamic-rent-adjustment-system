# Image Credits and Licensing

This document records the third-party visual assets and icon libraries used by the Dynamic Rent Adjustment System (DRAS), including their sources, applicable licenses, and intended use.

---

## 1. Generic Property Demo Placeholder

| Field | Details |
|---|---|
| **Asset Description** | Exterior photograph of a residential apartment building |
| **Provider** | [Unsplash](https://unsplash.com) |
| **Asset Identifier** | `photo-1545324418-cc1a3fa10c00` |
| **Direct Asset URL** | https://images.unsplash.com/photo-1545324418-cc1a3fa10c00 |
| **Optimized Display URL** | https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=1200&q=80 |
| **License** | [Unsplash License](https://unsplash.com/license) |
| **Permitted Use** | Personal and commercial use, subject to the license terms and restrictions |
| **Modifications** | Permitted under the applicable license |
| **Attribution** | Not required by the standard Unsplash License; appreciated where practicable |

**Intended Use**

- **Placeholder imagery:** Used as a generic fallback for demonstration listings that do not have property photographs.
- **No property representation:** The image is intended as illustrative imagery and must not be presented as a photograph of a specific property in the DRAS dataset.
- **User interface disclosure:** Where implemented, the interface identifies placeholder imagery using a label such as "Demo Preview" or "No photos uploaded."

**Source Verification Note:** The asset identifier and CDN URL identify the image file. The original photographer and Unsplash photo-page URL have not been verified and should be added if they can be established.

---

## 2. Local Deployment Demo Images (Pexels)

| Field | Details |
|---|---|
| **Asset Description** | 10 high-resolution architectural and apartment interior/exterior photos |
| **Provider** | [Pexels](https://www.pexels.com) |
| **Files** | `image1.jpg` through `image10.jpg` |
| **Local Deployment Path** | `uploads/images/demo/` |
| **Served Route** | `/api/images/demo/image1.jpg` - `/api/images/demo/image10.jpg` |
| **License** | [Pexels License](https://www.pexels.com/license/) |
| **Permitted Use** | Free to use for personal and commercial projects without prior permission |
| **Modifications** | Permitted under the Pexels License |
| **Attribution** | Not required; documented herein for project transparency |

**Intended Use**

- **Local & Offline Deployments:** Used to populate sample listings and demonstration environments so local instances run completely offline without external CDN dependencies.
- **Generic Architectural Photography:** Illustrative demo photography used for UI testing; does not depict specific properties in the Athens dataset.

---

## 3. Lucide Icons

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
